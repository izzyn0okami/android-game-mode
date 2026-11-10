package com.izzyn0okami.gamemode.diagnostic

import android.app.Activity
import android.content.Context
import android.hardware.input.InputManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.InputDevice
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.izzyn0okami.gamemode.ControllerClassifier
import com.izzyn0okami.gamemode.InputDeviceSnapshot

class MainActivity : Activity(), InputManager.InputDeviceListener {
    private lateinit var inputManager: InputManager
    private lateinit var summaryView: TextView
    private lateinit var devicesContainer: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        inputManager = getSystemService(Context.INPUT_SERVICE) as InputManager

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(20), dp(20), dp(20))
        }

        content.addView(TextView(this).apply {
            text = "Diagnóstico de controles"
            textSize = 24f
        })

        content.addView(TextView(this).apply {
            text = "Este aplicativo apenas observa dispositivos de entrada enquanto está aberto. Não inicia frontend nem altera o launcher."
            textSize = 14f
            setPadding(0, dp(8), 0, dp(16))
        })

        summaryView = TextView(this).apply {
            textSize = 18f
            setPadding(0, 0, 0, dp(12))
        }
        content.addView(summaryView)

        content.addView(TextView(this).apply {
            text = "Dispositivos encontrados"
            textSize = 18f
            setPadding(0, dp(8), 0, dp(8))
        })

        devicesContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        content.addView(
            devicesContainer,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ),
        )

        val scroll = ScrollView(this).apply { addView(content) }
        setContentView(scroll)
    }

    override fun onStart() {
        super.onStart()
        inputManager.registerInputDeviceListener(this, Handler(Looper.getMainLooper()))
        refreshDevices()
    }

    override fun onStop() {
        inputManager.unregisterInputDeviceListener(this)
        super.onStop()
    }

    override fun onInputDeviceAdded(deviceId: Int) = refreshDevices()

    override fun onInputDeviceRemoved(deviceId: Int) = refreshDevices()

    override fun onInputDeviceChanged(deviceId: Int) = refreshDevices()

    private fun refreshDevices() {
        val snapshots = inputManager.inputDeviceIds
            .asSequence()
            .mapNotNull { id -> inputManager.getInputDevice(id)?.toSnapshot() }
            .toList()
        val classified = snapshots.map { it to ControllerClassifier.classify(it) }
        val eligibleCount = classified.count { (_, result) -> result.eligible }

        summaryView.text = when (eligibleCount) {
            0 -> "Nenhum controle elegível detectado"
            1 -> "1 controle elegível detectado"
            else -> eligibleCount.toString() + " controles elegíveis detectados"
        }

        devicesContainer.removeAllViews()
        if (classified.isEmpty()) {
            devicesContainer.addView(TextView(this).apply {
                text = "Nenhum dispositivo de entrada encontrado."
                textSize = 14f
            })
            return
        }

        classified.forEach { (device, result) ->
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(12), dp(10), dp(12), dp(10))
            }
            card.addView(TextView(this).apply {
                text = (if (result.eligible) "CONTROLE" else "IGNORADO") + " · " + device.name
                textSize = 16f
            })
            card.addView(TextView(this).apply {
                text = buildString {
                    append("ID: ").append(device.id)
                    append("  •  Externo (USB/Bluetooth/periférico): ")
                    append(if (device.isExternal) "sim" else "não")
                    append("\nMotivo: ").append(result.reason)
                    append("\nFontes: 0x").append(device.sources.toUInt().toString(16).uppercase())
                    append("\nVendor ID: ").append(device.vendorId)
                    append("  •  Product ID: ").append(device.productId)
                }
                textSize = 13f
                setPadding(0, dp(4), 0, 0)
            })
            devicesContainer.addView(card)
            devicesContainer.addView(TextView(this).apply {
                text = "────────────────────────────────"
                textSize = 12f
            })
        }
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

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}