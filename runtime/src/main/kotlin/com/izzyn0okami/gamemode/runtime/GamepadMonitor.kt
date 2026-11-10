package com.izzyn0okami.gamemode.runtime

import android.content.Context
import android.hardware.input.InputManager
import android.os.Handler
import android.os.Looper
import android.view.InputDevice
import com.izzyn0okami.gamemode.ControllerClassifier
import com.izzyn0okami.gamemode.InputDeviceSnapshot

/**
 * Owns the Android input-device listener and emits a fresh, complete device
 * snapshot after startup and every add/remove/change event.
 *
 * Call [start] and [stop] from the main thread and pair them with the owner
 * lifecycle. The eventual ROM service will own this monitor for its lifetime.
 */
class GamepadMonitor(
    context: Context,
    private val onDevicesChanged: (List<ObservedInputDevice>) -> Unit,
) : InputManager.InputDeviceListener {

    private val inputManager = context.applicationContext
        .getSystemService(Context.INPUT_SERVICE) as InputManager
    private val mainHandler = Handler(Looper.getMainLooper())
    private var started = false

    fun start() {
        if (started) return
        started = true
        inputManager.registerInputDeviceListener(this, mainHandler)
        refreshDevices()
    }

    fun stop() {
        if (!started) return
        started = false
        inputManager.unregisterInputDeviceListener(this)
    }

    override fun onInputDeviceAdded(deviceId: Int) = refreshDevices()

    override fun onInputDeviceRemoved(deviceId: Int) = refreshDevices()

    override fun onInputDeviceChanged(deviceId: Int) = refreshDevices()

    private fun refreshDevices() {
        if (!started) return

        val devices = inputManager.inputDeviceIds
            .asSequence()
            .mapNotNull { id -> inputManager.getInputDevice(id)?.toSnapshot() }
            .map { snapshot ->
                ObservedInputDevice(
                    snapshot = snapshot,
                    classification = ControllerClassifier.classify(snapshot),
                )
            }
            .toList()

        onDevicesChanged(devices)
    }

    private fun InputDevice.toSnapshot() = InputDeviceSnapshot(
        id = id,
        name = name,
        sources = sources,
        supportsGamepad = supportsSource(InputDevice.SOURCE_GAMEPAD),
        supportsJoystick = supportsSource(InputDevice.SOURCE_JOYSTICK),
        isExternal = isExternal,
        isVirtual = isVirtual,
        vendorId = vendorId,
        productId = productId,
    )
}
