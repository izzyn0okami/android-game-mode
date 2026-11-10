package com.izzyn0okami.gamemode.runtime

import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import com.izzyn0okami.gamemode.GameModeCancellation
import com.izzyn0okami.gamemode.GameModeCoordinator
import com.izzyn0okami.gamemode.GameModeEffects
import com.izzyn0okami.gamemode.GameModeScheduler

/**
 * Android adapter for input-device monitoring and display/lock observation.
 * State transitions and asynchronous effect lifecycle are handled by the
 * platform-independent [GameModeCoordinator].
 *
 * All lifecycle methods and platform callbacks are serialized on the main thread.
 * This class deliberately does not launch Activities itself.
 */
class GameModeController(
    context: Context,
    private val actionHandler: GameModeActionHandler,
    exitGracePeriodMs: Long = GameModeCoordinator.DEFAULT_EXIT_GRACE_PERIOD_MS,
) {
    private val appContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())
    private val powerManager = appContext.getSystemService(Context.POWER_SERVICE) as PowerManager
    private val keyguardManager =
        appContext.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
    private var started = false

    private val coordinator = GameModeCoordinator(
        scheduler = object : GameModeScheduler {
            override fun post(task: () -> Unit) {
                mainHandler.post(task)
            }

            override fun postDelayed(delayMs: Long, task: () -> Unit): GameModeCancellation {
                val runnable = Runnable(task)
                mainHandler.postDelayed(runnable, delayMs)
                return GameModeCancellation { mainHandler.removeCallbacks(runnable) }
            }
        },
        effects = object : GameModeEffects {
            override fun launchFrontend(onResult: (success: Boolean) -> Unit) {
                actionHandler.launchFrontend(onResult)
            }

            override fun returnHome(onComplete: () -> Unit) {
                actionHandler.returnHome(onComplete)
            }
        },
        beforeHomeCompletion = { refreshDisplayAndLockState() },
        exitGracePeriodMs = exitGracePeriodMs,
    )

    private val gamepadMonitor = GamepadMonitor(appContext) { devices ->
        coordinator.onControllersChanged(devices.count { it.classification.eligible })
    }

    private val displayReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            refreshDisplayAndLockState()
        }
    }

    fun start() {
        checkMainThread()
        if (started) return

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        if (Build.VERSION.SDK_INT >= 33) {
            appContext.registerReceiver(displayReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("DEPRECATION")
            appContext.registerReceiver(displayReceiver, filter)
        }

        started = true
        coordinator.start()
        refreshDisplayAndLockState()
        gamepadMonitor.start()
    }

    fun stop() {
        checkMainThread()
        if (!started) return
        started = false
        coordinator.stop()
        gamepadMonitor.stop()
        appContext.unregisterReceiver(displayReceiver)
    }

    private fun refreshDisplayAndLockState() {
        checkMainThread()
        if (!started) return
        coordinator.onDisplayAndLockStateChanged(
            isInteractive = powerManager.isInteractive,
            isUnlocked = !keyguardManager.isDeviceLocked,
        )
    }

    private fun checkMainThread() {
        check(Looper.myLooper() == Looper.getMainLooper()) {
            "GameModeController must be used from the main thread"
        }
    }
}

/** Platform effects are injected so policy can be tested without launching real Activities. */
interface GameModeActionHandler {
    fun launchFrontend(onResult: (success: Boolean) -> Unit)
    fun returnHome(onComplete: () -> Unit)
}
