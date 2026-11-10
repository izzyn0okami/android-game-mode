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
        assertEquals(
            listOf(GameModeAction.ReturnToHome),
            machine.onExitGracePeriodElapsed(),
        )
        assertTrue(machine.onControllersChanged(1).isEmpty())
        assertEquals(
            listOf(GameModeAction.LaunchFrontend),
            machine.onReturnedToHome(),
        )
        assertEquals(GameModeState.ENTERING, machine.state)
    }

    @Test
    fun gracePeriodReturnsHomeOnlyWhenNoControllerRemains() {
        val machine = activeMachine()
        machine.onControllersChanged(0)
        assertEquals(listOf(GameModeAction.ReturnToHome), machine.onExitGracePeriodElapsed())
        assertEquals(GameModeState.EXITING, machine.state)
        machine.onReturnedToHome()
        assertEquals(GameModeState.IDLE, machine.state)
    }

    @Test
    fun failedFrontendLaunchResetsState() {
        val machine = GameModeStateMachine()
        machine.onDisplayAndLockStateChanged(true, true)
        machine.onControllersChanged(1)
        assertTrue(machine.onFrontendLaunchResult(false).isEmpty())
        assertEquals(GameModeState.IDLE, machine.state)
    }

    private fun activeMachine(): GameModeStateMachine {
        val machine = GameModeStateMachine()
        machine.onDisplayAndLockStateChanged(true, true)
        machine.onControllersChanged(1)
        machine.onFrontendLaunchResult(true)
        return machine
    }
}
