package com.izzyn0kami.gamemode

fun interface GameModeCancellation {
    fun cancel()
}

interface GameModeScheduler {
    fun post(task: () -> Unit)
    fun postDelayed(delayMs: Long, task: () -> Unit): GameModeCancellation
}

interface GameModeEffects {
    fun launchFrontend(onResult: (success: Boolean) -> Unit)
    fun returnHome(onComplete: () -> Unit)
}

/**
 * Platform-independent orchestration for state transitions, delayed exit, and
 * asynchronous effect callbacks. The Android owner supplies a main-thread scheduler.
 */
class GameModeCoordinator(
    private val scheduler: GameModeScheduler,
    private val effects: GameModeEffects,
    private val beforeHomeCompletion: () -> Unit = {},
    private val exitGracePeriodMs: Long = DEFAULT_EXIT_GRACE_PERIOD_MS,
) {
    private val stateMachine = GameModeStateMachine()
    private var started = false
    private var generation = 0L
    private var exitCancellation: GameModeCancellation? = null

    init {
        require(exitGracePeriodMs >= 0) { "Exit grace period cannot be negative" }
    }

    fun start() {
        if (started) return
        generation += 1
        started = true
    }

    fun stop() {
        if (!started) return
        started = false
        generation += 1
        cancelExit()
        stateMachine.reset()
    }

    fun onControllersChanged(count: Int) {
        if (started) dispatch(stateMachine.onControllersChanged(count))
    }

    fun onDisplayAndLockStateChanged(isInteractive: Boolean, isUnlocked: Boolean) {
        if (started) {
            dispatch(stateMachine.onDisplayAndLockStateChanged(isInteractive, isUnlocked))
        }
    }

    private fun dispatch(actions: List<GameModeAction>) {
        actions.forEach { action ->
            when (action) {
                GameModeAction.LaunchFrontend -> {
                    val callbackGeneration = generation
                    try {
                        effects.launchFrontend { success ->
                            scheduler.post {
                                if (isCurrent(callbackGeneration)) {
                                    dispatch(stateMachine.onFrontendLaunchResult(success))
                                }
                            }
                        }
                    } catch (_: Exception) {
                        dispatch(stateMachine.onFrontendLaunchResult(false))
                    }
                }

                GameModeAction.ScheduleExitGracePeriod -> {
                    cancelExit()
                    val callbackGeneration = generation
                    exitCancellation = scheduler.postDelayed(exitGracePeriodMs) {
                        exitCancellation = null
                        if (isCurrent(callbackGeneration)) {
                            dispatch(stateMachine.onExitGracePeriodElapsed())
                        }
                    }
                }

                GameModeAction.CancelExitGracePeriod -> cancelExit()

                GameModeAction.ReturnToHome -> {
                    val callbackGeneration = generation
                    try {
                        effects.returnHome {
                            scheduler.post {
                                if (isCurrent(callbackGeneration)) {
                                    beforeHomeCompletion()
                                    dispatch(stateMachine.onReturnedToHome())
                                }
                            }
                        }
                    } catch (_: Exception) {
                        // Remain in EXITING; never claim HOME navigation succeeded.
                    }
                }
            }
        }
    }

    private fun isCurrent(callbackGeneration: Long) =
        started && callbackGeneration == generation

    private fun cancelExit() {
        exitCancellation?.cancel()
        exitCancellation = null
    }

    companion object {
        const val DEFAULT_EXIT_GRACE_PERIOD_MS = 3_000L
    }
}
