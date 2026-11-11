package com.izzyn0okami.gamemode

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GameModeStateMachineTest {
    @Test
    fun controllerWaitsUntilScreenIsInteractiveAndUnlocked() {
        val machine = GameModeStateMachine()
        assertTrue(machine.onControllersChanged(1).isEmpty())
        assertEquals(GameModeState.WAITING_FOR_INTERACTIVE_UNLOCKED, machine.state)
        assertEquals(
            listOf(GameModeAction.LaunchFrontend),
            machine.onDisplayAndLockStateChanged(true, true)
        )
    }

    @Test
    fun disconnectingWhileWaitingReturnsToIdle() {
        val machine = GameModeStateMachine()
        machine.onControllersChanged(1)
        assertTrue(machine.onControllersChanged(0).isEmpty())
        assertEquals(GameModeState.IDLE, machine.state)
        machine.onDisplayAndLockStateChanged(true, true)
        assertEquals(
            listOf(GameModeAction.LaunchFrontend),
            machine.onControllersChanged(1),
        )
    }

    @Test
    fun multipleControllersDoNotLaunchDuplicateFrontend() {
        val machine = activeMachine()
        assertTrue(machine.onControllersChanged(2).isEmpty())
        assertEquals(GameModeState.ACTIVE, machine.state)
    }

    @Test
    fun removingOneOfSeveralControllersDoesNotExit() {
        val machine = activeMachine()
        assertTrue(machine.onControllersChanged(1).isEmpty())
        assertEquals(GameModeState.ACTIVE, machine.state)
    }

    @Test
    fun removingLastControllerSchedulesGracePeriod() {
        val machine = activeMachine()
        assertEquals(
            listOf(GameModeAction.ScheduleExitGracePeriod),
            machine.onControllersChanged(0)
        )
        assertEquals(GameModeState.EXIT_PENDING, machine.state)
    }

    @Test
    fun reconnectionCancelsPendingExit() {
        val machine = activeMachine()
        machine.onControllersChanged(0)
        assertEquals(
            listOf(GameModeAction.CancelExitGracePeriod),
            machine.onControllersChanged(1)
        )
        assertEquals(GameModeState.ACTIVE, machine.state)
        assertTrue(machine.onExitGracePeriodElapsed().isEmpty())
    }

    @Test
    fun reconnectionWhileReturningHomeReentersAfterHomeCompletes() {
        val machine = activeMachine()
        machine.onControllersChanged(0)
        machine.onExitGracePeriodElapsed()
        assertEquals(
            listOf(GameModeAction.ReturnToHome),
            machine.onExitPromptAnswered(ExitPromptResult.RETURN_HOME),
        )
        assertTrue(machine.onControllersChanged(1).isEmpty())
        assertEquals(
            listOf(GameModeAction.LaunchFrontend),
            machine.onReturnedToHome(),
        )
        assertEquals(GameModeState.ENTERING, machine.state)
    }

    @Test
    fun displayEventDuringGracePeriodDoesNotSkipTolerance() {
        val machine = activeMachine()
        machine.onControllersChanged(0)
        assertTrue(machine.onDisplayAndLockStateChanged(true, true).isEmpty())
        assertEquals(GameModeState.EXIT_PENDING, machine.state)
        assertEquals(listOf(GameModeAction.ShowExitPrompt), machine.onExitGracePeriodElapsed())
    }

    @Test
    fun gracePeriodAsksForConfirmationInsteadOfReturningHome() {
        val machine = activeMachine()
        machine.onControllersChanged(0)
        assertEquals(listOf(GameModeAction.ShowExitPrompt), machine.onExitGracePeriodElapsed())
        assertEquals(GameModeState.AWAITING_EXIT_CONFIRMATION, machine.state)
    }

    @Test
    fun gracePeriodDoesNotShowThePromptWhileScreenIsLockedOrOff() {
        val machine = activeMachine()
        machine.onControllersChanged(0)
        machine.onDisplayAndLockStateChanged(isInteractive = true, isUnlocked = false)
        assertTrue(machine.onExitGracePeriodElapsed().isEmpty())
        assertEquals(GameModeState.EXIT_PENDING, machine.state)

        assertEquals(
            listOf(GameModeAction.ShowExitPrompt),
            machine.onDisplayAndLockStateChanged(isInteractive = true, isUnlocked = true),
        )
        assertEquals(GameModeState.AWAITING_EXIT_CONFIRMATION, machine.state)
    }

    @Test
    fun gracePeriodWaitsForScreenToBecomeInteractiveBeforeShowingThePrompt() {
        val machine = activeMachine()
        machine.onControllersChanged(0)
        machine.onDisplayAndLockStateChanged(isInteractive = false, isUnlocked = true)
        assertTrue(machine.onExitGracePeriodElapsed().isEmpty())
        assertEquals(GameModeState.EXIT_PENDING, machine.state)

        assertEquals(
            listOf(GameModeAction.ShowExitPrompt),
            machine.onDisplayAndLockStateChanged(isInteractive = true, isUnlocked = true),
        )
    }

    @Test
    fun failedFrontendLaunchResetsState() {
        val machine = GameModeStateMachine()
        machine.onDisplayAndLockStateChanged(true, true)
        machine.onControllersChanged(1)
        assertTrue(machine.onFrontendLaunchResult(false).isEmpty())
        assertEquals(GameModeState.IDLE, machine.state)
    }

    @Test
    fun reconnectionAfterHomeWaitsIfScreenBecameLockedDuringNavigation() {
        val machine = activeMachine()
        machine.onControllersChanged(0)
        machine.onExitGracePeriodElapsed()
        machine.onExitPromptAnswered(ExitPromptResult.RETURN_HOME)
        machine.onControllersChanged(1)
        assertTrue(machine.onDisplayAndLockStateChanged(true, false).isEmpty())
        assertTrue(machine.onReturnedToHome().isEmpty())
        assertEquals(GameModeState.WAITING_FOR_INTERACTIVE_UNLOCKED, machine.state)
    }

    @Test
    fun resetClearsStateAndObservedPlatformValues() {
        val machine = activeMachine()
        machine.reset()
        assertEquals(GameModeState.IDLE, machine.state)
        assertTrue(machine.onControllersChanged(1).isEmpty())
        assertEquals(GameModeState.WAITING_FOR_INTERACTIVE_UNLOCKED, machine.state)
    }

    @Test
    fun resetDuringExitPendingDoesNotReturnHomeAfterRestart() {
        val machine = activeMachine()
        machine.onControllersChanged(0)
        machine.reset()
        assertTrue(machine.onExitGracePeriodElapsed().isEmpty())
        assertEquals(GameModeState.IDLE, machine.state)
    }

    @Test
    fun confirmingTheExitReturnsHomeAndThenIdles() {
        val machine = promptingMachine()
        assertEquals(
            listOf(GameModeAction.ReturnToHome),
            machine.onExitPromptAnswered(ExitPromptResult.RETURN_HOME),
        )
        assertEquals(GameModeState.EXITING, machine.state)
        machine.onReturnedToHome()
        assertEquals(GameModeState.IDLE, machine.state)
    }

    @Test
    fun decliningTheExitKeepsTheSessionAndDoesNotAskAgain() {
        val machine = promptingMachine()
        assertTrue(machine.onExitPromptAnswered(ExitPromptResult.STAY).isEmpty())
        assertEquals(GameModeState.EXIT_DECLINED, machine.state)
        assertTrue(machine.onExitGracePeriodElapsed().isEmpty())
        assertTrue(machine.onDisplayAndLockStateChanged(true, true).isEmpty())
        assertTrue(machine.onControllersChanged(0).isEmpty())
        assertEquals(GameModeState.EXIT_DECLINED, machine.state)
    }

    @Test
    fun reconnectingAfterDecliningResumesTheSessionAndPromptsAgainAfterTheNextDisconnect() {
        val machine = promptingMachine()
        machine.onExitPromptAnswered(ExitPromptResult.STAY)
        assertTrue(machine.onControllersChanged(1).isEmpty())
        assertEquals(GameModeState.ACTIVE, machine.state)
        assertEquals(
            listOf(GameModeAction.ScheduleExitGracePeriod),
            machine.onControllersChanged(0),
        )
        assertEquals(listOf(GameModeAction.ShowExitPrompt), machine.onExitGracePeriodElapsed())
    }

    @Test
    fun reconnectingWhileThePromptIsShownDismissesIt() {
        val machine = promptingMachine()
        assertEquals(listOf(GameModeAction.DismissExitPrompt), machine.onControllersChanged(1))
        assertEquals(GameModeState.ACTIVE, machine.state)
        assertTrue(machine.onExitPromptAnswered(ExitPromptResult.RETURN_HOME).isEmpty())
        assertEquals(GameModeState.ACTIVE, machine.state)
    }

    @Test
    fun lockingTheScreenWhileThePromptIsShownDismissesItAndAsksAgainOnUnlock() {
        val machine = promptingMachine()
        assertEquals(
            listOf(GameModeAction.DismissExitPrompt),
            machine.onDisplayAndLockStateChanged(isInteractive = true, isUnlocked = false),
        )
        assertEquals(GameModeState.EXIT_PENDING, machine.state)
        assertEquals(
            listOf(GameModeAction.ShowExitPrompt),
            machine.onDisplayAndLockStateChanged(isInteractive = true, isUnlocked = true),
        )
        assertEquals(GameModeState.AWAITING_EXIT_CONFIRMATION, machine.state)
    }

    @Test
    fun turningTheScreenOffWhileThePromptIsShownDismissesIt() {
        val machine = promptingMachine()
        assertEquals(
            listOf(GameModeAction.DismissExitPrompt),
            machine.onDisplayAndLockStateChanged(isInteractive = false, isUnlocked = true),
        )
        assertEquals(GameModeState.EXIT_PENDING, machine.state)
    }

    @Test
    fun answersWithoutAPendingPromptAreIgnored() {
        val machine = activeMachine()
        assertTrue(machine.onExitPromptAnswered(ExitPromptResult.RETURN_HOME).isEmpty())
        assertEquals(GameModeState.ACTIVE, machine.state)
    }

    @Test
    fun resetDuringThePromptIgnoresALateAnswer() {
        val machine = promptingMachine()
        machine.reset()
        assertTrue(machine.onExitPromptAnswered(ExitPromptResult.RETURN_HOME).isEmpty())
        assertEquals(GameModeState.IDLE, machine.state)
    }

    private fun promptingMachine(): GameModeStateMachine {
        val machine = activeMachine()
        machine.onControllersChanged(0)
        machine.onExitGracePeriodElapsed()
        check(machine.state == GameModeState.AWAITING_EXIT_CONFIRMATION)
        return machine
    }

    private fun activeMachine(): GameModeStateMachine {
        val machine = GameModeStateMachine()
        machine.onDisplayAndLockStateChanged(true, true)
        machine.onControllersChanged(1)
        machine.onFrontendLaunchResult(true)
        return machine
    }
}
