package com.izzyn0okami.gamemode

enum class GameModeState {
    IDLE,
    WAITING_FOR_INTERACTIVE_UNLOCKED,
    ENTERING,
    ACTIVE,
    EXIT_PENDING,
    AWAITING_EXIT_CONFIRMATION,
    EXIT_DECLINED,
    EXITING,
}

sealed interface GameModeAction {
    data object LaunchFrontend : GameModeAction
    data object ScheduleExitGracePeriod : GameModeAction
    data object CancelExitGracePeriod : GameModeAction
    data object ShowExitPrompt : GameModeAction
    data object DismissExitPrompt : GameModeAction
    data object ReturnToHome : GameModeAction
}

/** The user's answer to the prompt shown after the last controller disconnects. */
enum class ExitPromptResult { STAY, RETURN_HOME }

/** Platform-independent transitions; Android services and timers stay outside this class. */
class GameModeStateMachine {
    var state: GameModeState = GameModeState.IDLE
        private set
    private var eligibleControllerCount = 0
    private var interactive = false
    private var unlocked = false
    private var exitGraceElapsed = false

    /** Clears all observed platform state when the owning controller is stopped. */
    fun reset() {
        state = GameModeState.IDLE
        eligibleControllerCount = 0
        interactive = false
        unlocked = false
        exitGraceElapsed = false
    }

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
            count > 0 && state == GameModeState.AWAITING_EXIT_CONFIRMATION -> {
                state = GameModeState.ACTIVE
                listOf(GameModeAction.DismissExitPrompt)
            }
            count > 0 && state == GameModeState.EXIT_DECLINED -> {
                state = GameModeState.ACTIVE
                emptyList()
            }
            count == 0 && state == GameModeState.WAITING_FOR_INTERACTIVE_UNLOCKED -> {
                state = GameModeState.IDLE
                emptyList()
            }
            count == 0 && state == GameModeState.ACTIVE -> {
                state = GameModeState.EXIT_PENDING
                exitGraceElapsed = false
                listOf(GameModeAction.ScheduleExitGracePeriod)
            }
            else -> emptyList()
        }
    }

    fun onDisplayAndLockStateChanged(
        isInteractive: Boolean,
        isUnlocked: Boolean,
    ): List<GameModeAction> {
        interactive = isInteractive
        unlocked = isUnlocked
        return when {
            eligibleControllerCount > 0 &&
                state == GameModeState.WAITING_FOR_INTERACTIVE_UNLOCKED -> tryEnter()
            eligibleControllerCount == 0 &&
                state == GameModeState.EXIT_PENDING &&
                exitGraceElapsed &&
                interactive && unlocked -> promptForExit()
            state == GameModeState.AWAITING_EXIT_CONFIRMATION &&
                !(interactive && unlocked) -> {
                // The prompt never stays over a locked or sleeping display; ask again later.
                state = GameModeState.EXIT_PENDING
                listOf(GameModeAction.DismissExitPrompt)
            }
            else -> emptyList()
        }
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
            exitGraceElapsed = false
            listOf(GameModeAction.ScheduleExitGracePeriod)
        }
    }

    fun onExitGracePeriodElapsed(): List<GameModeAction> {
        if (state != GameModeState.EXIT_PENDING || eligibleControllerCount > 0) {
            return emptyList()
        }
        exitGraceElapsed = true
        // Never show UI while the display is off or the keyguard is showing.
        return if (interactive && unlocked) promptForExit() else emptyList()
    }

    /**
     * Answers the exit prompt. Answers that arrive when no prompt is pending are ignored.
     * An implementation that cannot present the prompt must answer [ExitPromptResult.STAY].
     */
    fun onExitPromptAnswered(result: ExitPromptResult): List<GameModeAction> {
        if (state != GameModeState.AWAITING_EXIT_CONFIRMATION) return emptyList()
        return when (result) {
            ExitPromptResult.RETURN_HOME -> beginExit()
            ExitPromptResult.STAY -> {
                state = GameModeState.EXIT_DECLINED
                emptyList()
            }
        }
    }

    fun onReturnedToHome(): List<GameModeAction> {
        if (state != GameModeState.EXITING) return emptyList()
        state = GameModeState.IDLE
        return if (eligibleControllerCount > 0) tryEnter() else emptyList()
    }

    private fun promptForExit(): List<GameModeAction> {
        state = GameModeState.AWAITING_EXIT_CONFIRMATION
        return listOf(GameModeAction.ShowExitPrompt)
    }

    private fun beginExit(): List<GameModeAction> {
        state = GameModeState.EXITING
        return listOf(GameModeAction.ReturnToHome)
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
