package com.izzyn0okami.gamemode

/**
 * Platform-independent snapshot of the public input-device properties used by
 * the diagnostic app. Keeping Android types out of core makes classification
 * deterministic and unit-testable.
 */
data class InputDeviceSnapshot(
    val id: Int,
    val name: String,
    val sources: Int,
    val supportsGamepad: Boolean,
    val supportsJoystick: Boolean,
    val isExternal: Boolean,
    val isVirtual: Boolean,
    val vendorId: Int,
    val productId: Int,
)

data class ControllerClassification(
    val eligible: Boolean,
    val reason: String,
)

object ControllerClassifier {
    fun classify(device: InputDeviceSnapshot): ControllerClassification = when {
        device.isVirtual -> ControllerClassification(false, "Dispositivo virtual")
        !device.isExternal -> ControllerClassification(false, "Dispositivo interno")
        device.supportsGamepad || device.supportsJoystick ->
            ControllerClassification(true, "Controle externo compatível")
        else -> ControllerClassification(false, "Não anuncia fonte GAMEPAD ou JOYSTICK")
    }
}
