package com.izzyn0okami.gamemode.diagnostic

import android.app.Activity
import android.os.Bundle
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.izzyn0okami.gamemode.runtime.GameModeActionHandler
import com.izzyn0okami.gamemode.runtime.GameModeController
import com.izzyn0okami.gamemode.runtime.ObservedInputDevice

/**
 * On-device integration harness for GameModeController.
 *
 * Frontend and HOME effects are deliberately simulated: this APK validates
 * controller orchestration without launching another app or changing HOME.
 */
class MainActivity : Activity() {
    private lateinit var controller: GameModeController
    private lateinit var summaryView: TextView
    private lateinit var actionView: TextView
    private lateinit var devicesContainer: LinearLayout
    private lateinit var logContainer: LinearLayout
    private val eventLog = ArrayDeque<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(20), dp(20), dp(20))
        }

        content.addView(TextView(this).apply {
            text = "Game Mode — diagnóstico integrado"
            textSize = 23f
        })
        content.addView(TextView(this).apply {
            text = "O controlador real está ativo enquanto esta tela permanece aberta. As ações de frontend e HOME são simuladas: nenhum aplicativo externo será aberto e o launcher padrão não será alterado."
            textSize = 14f
            setPadding(0, dp(8), 0, dp(16))
        })

        summaryView = TextView(this).apply {
            textSize = 18f
            setPadding(0, 0, 0, dp(8))
        }
        content.addView(summaryView)

        actionView = TextView(this).apply {
            text = "Aguardando o controlador…"
            textSize = 14f
            setPadding(0, 0, 0, dp(16))
        }
        content.addView(actionView)

        content.addView(sectionTitle("Dispositivos encontrados"))
        devicesContainer = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        content.addView(devicesContainer)

        content.addView(sectionTitle("Eventos do Game Mode"))
        logContainer = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        content.addView(logContainer)

        setContentView(ScrollView(this).apply { addView(content) })

        controller = GameModeController(
            context = applicationContext,
            actionHandler = object : GameModeActionHandler {
                override fun launchFrontend(onResult: (success: Boolean) -> Unit) {
                    actionView.text = "Frontend: solicitação simulada (nenhum app externo foi aberto)"
                    appendEvent("Controle conectado: o controlador solicitou iniciar o frontend (simulação).")
                    onResult(true)
                }

                override fun returnHome(onComplete: () -> Unit) {
                    actionView.text = "HOME: retorno simulado (launcher não foi alterado)"
                    appendEvent("Último controle desconectado: tolerância concluída; retorno ao HOME simulado.")
                    onComplete()
                }
            },
        )
        summaryView.text = "Controlador pronto; aguardando início do ciclo de vida."
    }

    override fun onStart() {
        super.onStart()
        controller.start()
        appendEvent("Controlador iniciado. Monitoramento de controles ativo.")
    }

    override fun onStop() {
        controller.stop()
        appendEvent("Controlador parado ao sair da tela de diagnóstico.")
        super.onStop()
    }

    private fun renderDevices(devices: List<ObservedInputDevice>) {
        val eligibleCount = devices.count { it.classification.eligible }
        summaryView.text = when (eligibleCount) {
            0 -> "Nenhum controle elegível conectado"
            1 -> "1 controle elegível conectado"
            else -> "$eligibleCount controles elegíveis conectados"
        }

        devicesContainer.removeAllViews()
        if (devices.isEmpty()) {
            devicesContainer.addView(TextView(this).apply {
                text = "Nenhum dispositivo de entrada encontrado."
                textSize = 14f
            })
            return
        }

        devices.forEach { observed ->
            val device = observed.snapshot
            val result = observed.classification
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
                    append("  •  Externo: ").append(if (device.isExternal) "sim" else "não")
                    append("\\nMotivo: ").append(result.reason)
                    append("\\nFontes: 0x").append(device.sources.toUInt().toString(16).uppercase())
                    append("\\nVendor ID: ").append(device.vendorId)
                    append("  •  Product ID: ").append(device.productId)
                }
                textSize = 13f
                setPadding(0, dp(4), 0, 0)
            })
            devicesContainer.addView(card)
        }
    }

    private fun appendEvent(message: String) {
        eventLog.addFirst(message)
        while (eventLog.size > 20) eventLog.removeLast()
        logContainer.removeAllViews()
        eventLog.forEach { entry ->
            logContainer.addView(TextView(this).apply {
                text = "• $entry"
                textSize = 13f
                setPadding(0, dp(3), 0, dp(3))
            })
        }
    }

    private fun sectionTitle(value: String) = TextView(this).apply {
        text = value
        textSize = 18f
        setPadding(0, dp(12), 0, dp(8))
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
