package com.izzyn0okami.gamemode

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ControllerClassifierTest {
    @Test
    fun externalGamepadIsEligible() {
        val result = ControllerClassifier.classify(device(supportsGamepad = true))
        assertTrue(result.eligible)
        assertEquals("Controle externo compatível", result.reason)
    }

    @Test
    fun externalJoystickIsEligible() {
        assertTrue(ControllerClassifier.classify(device(supportsJoystick = true)).eligible)
    }

    @Test
    fun keyboardOrMouseWithoutGameSourcesIsIgnored() {
        val result = ControllerClassifier.classify(device())
        assertFalse(result.eligible)
        assertEquals("Não anuncia fonte GAMEPAD ou JOYSTICK", result.reason)
    }

    @Test
    fun internalDeviceIsIgnoredEvenIfItAdvertisesGamepad() {
        val result = ControllerClassifier.classify(device(supportsGamepad = true, isExternal = false))
        assertFalse(result.eligible)
        assertEquals("Dispositivo interno", result.reason)
    }

    @Test
    fun virtualDeviceIsIgnoredEvenIfItAdvertisesJoystick() {
        val result = ControllerClassifier.classify(device(supportsJoystick = true, isVirtual = true))
        assertFalse(result.eligible)
        assertEquals("Dispositivo virtual", result.reason)
    }

    @Test
    fun combinedGamepadAndKeyboardSourcesRemainEligible() {
        assertTrue(ControllerClassifier.classify(device(supportsGamepad = true)).eligible)
    }

    private fun device(
        supportsGamepad: Boolean = false,
        supportsJoystick: Boolean = false,
        isExternal: Boolean = true,
        isVirtual: Boolean = false,
    ) = InputDeviceSnapshot(
        id = 1,
        name = "Test input",
        sources = 0,
        supportsGamepad = supportsGamepad,
        supportsJoystick = supportsJoystick,
        isExternal = isExternal,
        isVirtual = isVirtual,
        vendorId = 0,
        productId = 0,
    )
}
