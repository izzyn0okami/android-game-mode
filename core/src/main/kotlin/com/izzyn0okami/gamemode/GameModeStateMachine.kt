package com.izzyn0okami.gamemode

enum class GameModeState {
    IDLE, WAITING_FOR_INTERACTIVE_UNLOCKED, ENTERING, ACTIVE, EXIT_PENDING, EXITING
}

sealed interface GameModeAction {
    data object LaunchFrontend : GameModeAction
    data object ScheduleExitGracePeriod : GameModeAction
    data object CancelExitGracePeriod : GameModeAction
    data object ReturnToHome : GameModeAction
}

/** Platform-independent transitions; Android services and timers stay outside this class. */
class GameModeStateMachine {
    var state: GameModeState = GameModeState.IDLE
        private set
    private var eligibleControllerCount = 0
    private var interactive = false
    private var unlocked = false

    fun onControllersChanged(count: Int): List<GameModeAction> {
        require(count >= 0) { "Controller count cannot be negative" }
        eligibleControllerCount = count
        return when {
            count > 0 && (state == GameModeState.IDLE ||
                state == GameModeState.WAITING_FOR_INTERACTIVE_UNLOCKED) -> tryEnter()
            count > 0 && state == GameModeState.EXIT_PENDING -> {
                state = GameModeState.ACTIVE
                listOf(GameModeAction.CancelExitGracePeriod)
            }
            count == 0 && state == GameModeState.ACTIVE -> {
                state = GameModeState.EXIT_PENDING
                listOf(GameModeAction.ScheduleExitGracePeriod)
            }
            else -> emptyList()
        }
    }

    fun onDisplayAndLockStateChanged(isInteractive: Boolean, isUnlocked: Boolean): List<GameModeAction> {
        interactive = isInteractive
        unlocked = isUnlocked
        return if (eligibleControllerCount > 0 &&
            state == GameModeState.WAITING_FOR_INTERACTIVE_UNLOCKED) tryEnter() else emptyList()
    }

    fun onFrontendLaunchResult(success: Boolean): List<GameModeAction> {
        if (state != GameModeState.ENTERING) return emptyList()
        if (!success) {
            state = GameModeState.IDLE
            return emptyList()
        }
        return if (eligibleControllerCount > 0) {
            state = GameModeState.ACTIVE
            emptyList()
        } else {
            state = GameModeState.EXIT_PENDING
            listOf(GameModeAction.ScheduleExitGracePeriod)
        }
    }

    fun onExitGracePeriodElapsed(): List<GameModeAction> {
        if (state != GameModeState.EXIT_PENDING || eligibleControllerCount > 0) return emptyList()
        state = GameModeState.EXITING
        return listOf(GameModeAction.ReturnToHome)
    }

    fun onReturnedToHome() {
        if (state == GameModeState.EXITING) state = GameModeState.IDLE
    }

    private fun tryEnter(): List<GameModeAction> {
        if (eligibleControllerCount == 0) return emptyList()
        if (!interactive || !unlocked) {
            state = GameModeState.WAITING_FOR_INTERACTIVE_UNLOCKED
            return emptyList()
        }
        state = GameModeState.ENTERING
        return listOf(GameModeAction.LaunchFrontend)
    }
}
