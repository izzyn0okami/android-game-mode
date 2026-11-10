package com.izzyn0okami.gamemode

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GameModeCoordinatorTest {
    @Test
    fun waitsForInteractiveUnlockedScreenAndLaunchesOnlyOnce() {
        val scheduler = FakeScheduler()
        val effects = FakeEffects()
        val coordinator = GameModeCoordinator(scheduler, effects)

        coordinator.start()
        coordinator.onControllersChanged(1)
        coordinator.onControllersChanged(2)
        assertEquals(0, effects.launchCallbacks.size)

        coordinator.onDisplayAndLockStateChanged(true, false)
        assertEquals(0, effects.launchCallbacks.size)
        coordinator.onDisplayAndLockStateChanged(true, true)
        assertEquals(1, effects.launchCallbacks.size)
    }

    @Test
    fun reconnectionCancelsGracePeriod() {
        val scheduler = FakeScheduler()
        val effects = FakeEffects()
        val coordinator = activeCoordinator(scheduler, effects)

        coordinator.onControllersChanged(0)
        assertEquals(1, scheduler.pendingDelayedCount)
        coordinator.onControllersChanged(1)
        scheduler.advanceBy(3_000)

        assertEquals(0, effects.homeCallbacks.size)
    }

    @Test
    fun gracePeriodRequestsHomeOnceAndCompletesAsynchronously() {
        val scheduler = FakeScheduler()
        val effects = FakeEffects()
        var beforeHomeCompletionCount = 0
        val coordinator = GameModeCoordinator(
            scheduler = scheduler,
            effects = effects,
            beforeHomeCompletion = { beforeHomeCompletionCount++ },
            exitGracePeriodMs = 250,
        )
        coordinator.start()
        coordinator.onDisplayAndLockStateChanged(true, true)
        coordinator.onControllersChanged(1)
        effects.launchCallbacks.single()(true)
        scheduler.runCurrent()

        coordinator.onControllersChanged(0)
        scheduler.advanceBy(249)
        assertEquals(0, effects.homeCallbacks.size)
        scheduler.advanceBy(1)
        assertEquals(1, effects.homeCallbacks.size)

        effects.homeCallbacks.single()()
        assertEquals(0, beforeHomeCompletionCount)
        scheduler.runCurrent()
        assertEquals(1, beforeHomeCompletionCount)
    }

    @Test
    fun homeReturnWaitsUntilDeviceIsUnlockedAfterGracePeriod() {
        val scheduler = FakeScheduler()
        val effects = FakeEffects()
        val coordinator = activeCoordinator(scheduler, effects)

        coordinator.onControllersChanged(0)
        coordinator.onDisplayAndLockStateChanged(isInteractive = true, isUnlocked = false)
        scheduler.advanceBy(3_000)
        assertEquals(0, effects.homeCallbacks.size)

        coordinator.onDisplayAndLockStateChanged(isInteractive = true, isUnlocked = true)
        scheduler.runCurrent()
        assertEquals(1, effects.homeCallbacks.size)
    }

    @Test
    fun stopIgnoresLateLaunchCallbackAfterRestart() {
        val scheduler = FakeScheduler()
        val effects = FakeEffects()
        val coordinator = GameModeCoordinator(scheduler, effects)

        coordinator.start()
        coordinator.onDisplayAndLockStateChanged(true, true)
        coordinator.onControllersChanged(1)
        val staleLaunch = effects.launchCallbacks.single()

        coordinator.stop()
        coordinator.start()
        staleLaunch(true)
        scheduler.runCurrent()

        coordinator.onControllersChanged(1)
        assertEquals(1, effects.launchCallbacks.size)
        assertEquals(0, effects.homeCallbacks.size)
    }

    @Test
    fun cancelledStaleTimerCannotExpireANewerGracePeriod() {
        val scheduler = FakeScheduler()
        val effects = FakeEffects()
        val coordinator = activeCoordinator(scheduler, effects)

        coordinator.onControllersChanged(0)
        coordinator.onControllersChanged(1)
        coordinator.onControllersChanged(0)

        scheduler.runCancelledDelayedTasks()
        assertEquals(0, effects.homeCallbacks.size)
        assertEquals(1, scheduler.pendingDelayedCount)

        scheduler.advanceBy(2_999)
        assertEquals(0, effects.homeCallbacks.size)
        scheduler.advanceBy(1)
        assertEquals(1, effects.homeCallbacks.size)
    }

    @Test
    fun stopCancelsPendingGracePeriod() {
        val scheduler = FakeScheduler()
        val effects = FakeEffects()
        val coordinator = activeCoordinator(scheduler, effects)

        coordinator.onControllersChanged(0)
        assertEquals(1, scheduler.pendingDelayedCount)
        coordinator.stop()
        assertEquals(0, scheduler.pendingDelayedCount)
        scheduler.advanceBy(3_000)

        assertEquals(0, effects.homeCallbacks.size)
    }

    @Test
    fun stopIgnoresLateHomeCompletionAfterRestart() {
        val scheduler = FakeScheduler()
        val effects = FakeEffects()
        val coordinator = activeCoordinator(scheduler, effects)

        coordinator.onControllersChanged(0)
        scheduler.advanceBy(3_000)
        val staleHome = effects.homeCallbacks.single()

        coordinator.stop()
        coordinator.start()
        staleHome()
        scheduler.runCurrent()

        assertEquals(1, effects.launchCallbacks.size)
    }

    @Test
    fun failedLaunchCanBeRetriedWithoutDuplicateActiveState() {
        val scheduler = FakeScheduler()
        val effects = FakeEffects()
        val coordinator = GameModeCoordinator(scheduler, effects)
        coordinator.start()
        coordinator.onDisplayAndLockStateChanged(true, true)
        coordinator.onControllersChanged(1)

        effects.launchCallbacks.single()(false)
        scheduler.runCurrent()
        coordinator.onControllersChanged(1)

        assertEquals(2, effects.launchCallbacks.size)
    }

    @Test
    fun displayEventDuringGracePeriodDoesNotRequestHomeEarly() {
        val scheduler = FakeScheduler()
        val effects = FakeEffects()
        val coordinator = activeCoordinator(scheduler, effects)

        coordinator.onControllersChanged(0)
        coordinator.onDisplayAndLockStateChanged(isInteractive = true, isUnlocked = true)
        assertEquals(0, effects.homeCallbacks.size)

        scheduler.advanceBy(2_999)
        assertEquals(0, effects.homeCallbacks.size)
        scheduler.advanceBy(1)
        assertEquals(1, effects.homeCallbacks.size)
    }

    @Test
    fun gracePeriodShowsThePromptAndWaitsForTheAnswer() {
        val scheduler = FakeScheduler()
        val effects = FakeEffects().apply { promptAnswer = null }
        val coordinator = activeCoordinator(scheduler, effects)

        coordinator.onControllersChanged(0)
        scheduler.advanceBy(3_000)
        assertEquals(1, effects.promptCallbacks.size)
        assertEquals(0, effects.homeCallbacks.size)

        scheduler.advanceBy(60_000)
        assertEquals(0, effects.homeCallbacks.size)

        effects.promptCallbacks.single()(ExitPromptResult.RETURN_HOME)
        scheduler.runCurrent()
        assertEquals(1, effects.homeCallbacks.size)
    }

    @Test
    fun decliningThePromptKeepsTheSessionAndDoesNotAskAgain() {
        val scheduler = FakeScheduler()
        val effects = FakeEffects().apply { promptAnswer = null }
        val coordinator = activeCoordinator(scheduler, effects)

        coordinator.onControllersChanged(0)
        scheduler.advanceBy(3_000)
        effects.promptCallbacks.single()(ExitPromptResult.STAY)
        scheduler.runCurrent()
        scheduler.advanceBy(60_000)

        assertEquals(0, effects.homeCallbacks.size)
        assertEquals(1, effects.promptCallbacks.size)
    }

    @Test
    fun reconnectionDismissesTheVisiblePromptAndIgnoresItsLateAnswer() {
        val scheduler = FakeScheduler()
        val effects = FakeEffects().apply { promptAnswer = null }
        val coordinator = activeCoordinator(scheduler, effects)

        coordinator.onControllersChanged(0)
        scheduler.advanceBy(3_000)
        val latePrompt = effects.promptCallbacks.single()

        coordinator.onControllersChanged(1)
        assertEquals(1, effects.dismissedPromptCount)
        latePrompt(ExitPromptResult.RETURN_HOME)
        scheduler.runCurrent()

        assertEquals(0, effects.homeCallbacks.size)
    }

    @Test
    fun answerToAnEarlierPromptCannotAnswerANewerOne() {
        val scheduler = FakeScheduler()
        val effects = FakeEffects().apply { promptAnswer = null }
        val coordinator = activeCoordinator(scheduler, effects)

        coordinator.onControllersChanged(0)
        scheduler.advanceBy(3_000)
        val firstPrompt = effects.promptCallbacks.single()
        coordinator.onControllersChanged(1)
        coordinator.onControllersChanged(0)
        scheduler.advanceBy(3_000)
        assertEquals(2, effects.promptCallbacks.size)

        firstPrompt(ExitPromptResult.RETURN_HOME)
        scheduler.runCurrent()
        assertEquals(0, effects.homeCallbacks.size)

        effects.promptCallbacks.last()(ExitPromptResult.RETURN_HOME)
        scheduler.runCurrent()
        assertEquals(1, effects.homeCallbacks.size)
    }

    @Test
    fun stopDismissesTheVisiblePromptAndIgnoresItsLateAnswer() {
        val scheduler = FakeScheduler()
        val effects = FakeEffects().apply { promptAnswer = null }
        val coordinator = activeCoordinator(scheduler, effects)

        coordinator.onControllersChanged(0)
        scheduler.advanceBy(3_000)
        val latePrompt = effects.promptCallbacks.single()

        coordinator.stop()
        assertEquals(1, effects.dismissedPromptCount)
        coordinator.start()
        latePrompt(ExitPromptResult.RETURN_HOME)
        scheduler.runCurrent()

        assertEquals(0, effects.homeCallbacks.size)
    }

    @Test
    fun promptThatCannotBeShownKeepsTheSession() {
        val scheduler = FakeScheduler()
        val effects = FakeEffects().apply { promptFailure = IllegalStateException("no window") }
        val coordinator = activeCoordinator(scheduler, effects)

        coordinator.onControllersChanged(0)
        scheduler.advanceBy(3_000)
        scheduler.advanceBy(60_000)

        assertEquals(0, effects.homeCallbacks.size)
        assertEquals(0, effects.promptCallbacks.size)
    }

    @Test
    fun lockingTheScreenDismissesThePromptAndAsksAgainOnUnlock() {
        val scheduler = FakeScheduler()
        val effects = FakeEffects().apply { promptAnswer = null }
        val coordinator = activeCoordinator(scheduler, effects)

        coordinator.onControllersChanged(0)
        scheduler.advanceBy(3_000)
        assertEquals(1, effects.promptCallbacks.size)

        coordinator.onDisplayAndLockStateChanged(isInteractive = true, isUnlocked = false)
        assertEquals(1, effects.dismissedPromptCount)
        assertEquals(1, effects.promptCallbacks.size)

        coordinator.onDisplayAndLockStateChanged(isInteractive = true, isUnlocked = true)
        assertEquals(2, effects.promptCallbacks.size)
        assertEquals(0, effects.homeCallbacks.size)
    }

    @Test
    fun effectsWithoutPromptUiReturnHomeRightAfterTheGracePeriod() {
        val scheduler = FakeScheduler()
        val homeCallbacks = mutableListOf<() -> Unit>()
        var launchCallback: ((Boolean) -> Unit)? = null
        val effects = object : GameModeEffects {
            override fun launchFrontend(onResult: (success: Boolean) -> Unit) {
                launchCallback = onResult
            }

            override fun returnHome(onComplete: () -> Unit) {
                homeCallbacks += onComplete
            }
        }
        val coordinator = GameModeCoordinator(scheduler, effects)
        coordinator.start()
        coordinator.onDisplayAndLockStateChanged(true, true)
        coordinator.onControllersChanged(1)
        launchCallback!!.invoke(true)
        scheduler.runCurrent()

        coordinator.onControllersChanged(0)
        scheduler.advanceBy(3_000)

        assertEquals(1, homeCallbacks.size)
    }

    private fun activeCoordinator(
        scheduler: FakeScheduler,
        effects: FakeEffects,
    ): GameModeCoordinator {
        val coordinator = GameModeCoordinator(scheduler, effects)
        coordinator.start()
        coordinator.onDisplayAndLockStateChanged(true, true)
        coordinator.onControllersChanged(1)
        effects.launchCallbacks.single()(true)
        scheduler.runCurrent()
        return coordinator
    }

    private class FakeEffects : GameModeEffects {
        val launchCallbacks = mutableListOf<(Boolean) -> Unit>()
        val homeCallbacks = mutableListOf<() -> Unit>()
        val promptCallbacks = mutableListOf<(ExitPromptResult) -> Unit>()
        var dismissedPromptCount = 0
        var promptAnswer: ExitPromptResult? = ExitPromptResult.RETURN_HOME
        var promptFailure: Exception? = null

        override fun launchFrontend(onResult: (success: Boolean) -> Unit) {
            launchCallbacks += onResult
        }

        override fun returnHome(onComplete: () -> Unit) {
            homeCallbacks += onComplete
        }

        override fun showExitPrompt(onResult: (ExitPromptResult) -> Unit) {
            promptFailure?.let { throw it }
            promptCallbacks += onResult
            promptAnswer?.let { onResult(it) }
        }

        override fun dismissExitPrompt() {
            dismissedPromptCount++
        }
    }

    private class FakeScheduler : GameModeScheduler {
        private data class DelayedTask(
            val at: Long,
            val task: () -> Unit,
            var cancelled: Boolean = false,
            var forced: Boolean = false,
        )

        private val queued = ArrayDeque<() -> Unit>()
        private val delayed = mutableListOf<DelayedTask>()
        private var now = 0L

        val pendingDelayedCount: Int
            get() = delayed.count { !it.cancelled }

        override fun post(task: () -> Unit) {
            queued.addLast(task)
        }

        override fun postDelayed(delayMs: Long, task: () -> Unit): GameModeCancellation {
            val entry = DelayedTask(now + delayMs, task)
            delayed += entry
            return GameModeCancellation { entry.cancelled = true }
        }

        fun advanceBy(deltaMs: Long) {
            now += deltaMs
            delayed.filter { !it.cancelled && it.at <= now }
                .sortedBy { it.at }
                .forEach {
                    it.cancelled = true
                    queued.addLast(it.task)
                }
            runCurrent()
        }

        fun runCancelledDelayedTasks() {
            delayed.filter { it.cancelled && !it.forced }.forEach {
                it.forced = true
                queued.addLast(it.task)
            }
            runCurrent()
        }

        fun runCurrent() {
            while (queued.isNotEmpty()) queued.removeFirst().invoke()
        }
    }
}
