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
    fun stopCancelsTimerAndIgnoresLateLaunchCallbackAfterRestart() {
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

        override fun launchFrontend(onResult: (success: Boolean) -> Unit) {
            launchCallbacks += onResult
        }

        override fun returnHome(onComplete: () -> Unit) {
            homeCallbacks += onComplete
        }
    }

    private class FakeScheduler : GameModeScheduler {
        private data class DelayedTask(
            val at: Long,
            val task: () -> Unit,
            var cancelled: Boolean = false,
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

        fun runCurrent() {
            while (queued.isNotEmpty()) queued.removeFirst().invoke()
        }
    }
}
