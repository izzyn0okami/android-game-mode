package com.izzyn0okami.gamemode.runtime

import com.izzyn0okami.gamemode.ControllerClassification
import com.izzyn0okami.gamemode.InputDeviceSnapshot

/** A device snapshot together with the platform-independent eligibility decision. */
data class ObservedInputDevice(
    val snapshot: InputDeviceSnapshot,
    val classification: ControllerClassification,
)
