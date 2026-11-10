package com.izzyn0okami.gamemode.runtime

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.app.KeyguardManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import com.izzyn0okami.gamemode.GameModeAction
import com.izzyn0okami.gamemode.GameModeStateMachine

/**
 * Android-side coordinator for gamepad, display/lock state, grace-period timing,
 * and the platform-specific effects supplied by [GameModeActionHandler].
 *
 * All lifecycle methods and callbacks are serialized on the main thread.
 * This class deliberately does not launch Activities itself: the ROM integration
 * must provide an executor whose background-activity-launch privileges are verified.
 */
class GameModeController(
    context: Context,
    private val actionHandler: GameModeActionHandler,
    private val exitGracePeriodMs: Long = DEFAULT_EXIT_GRACE_PERIOD_MS,
) {
    private val appContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())
    private val stateMachine = GameModeStateMachine()
    private val powerManager = appContext.getSystemService(Context.POWER_SERVICE) as PowerManager
    private val keyguardManager = appContext.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
    private var started = false
    private var lifecycleGeneration = 0L
    private var exitTask: Runnable? = null

    private val gamepadMonitor = GamepadMonitor(appContext) { devices ->
        dispatch(stateMachine.onControllersChanged(devices.count { it.classification.eligible }))
    }

    private val displayReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            refreshDisplayAndLockState()
        }
    }

    init {
        require(exitGracePeriodMs >= 0) { "Exit grace period cannot be negative" }
    }

    fun start() {
        checkMainThread()
        if (started) return
        lifecycleGeneration += 1
        started = true

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

        refreshDisplayAndLockState()
        gamepadMonitor.start()
    }

    fun stop() {
        checkMainThread()
        if (!started) return
        started = false
        lifecycleGeneration += 1
        cancelExitTask()
        gamepadMonitor.stop()
        stateMachine.reset()
        appContext.unregisterReceiver(displayReceiver)
    }

    private fun refreshDisplayAndLockState() {
        checkMainThread()
        if (!started) return
        dispatch(
            stateMachine.onDisplayAndLockStateChanged(
                isInteractive = powerManager.isInteractive,
                isUnlocked = !keyguardManager.isDeviceLocked,
            ),
        )
    }

    private fun dispatch(actions: List<GameModeAction>) {
        checkMainThread()
        actions.forEach { action ->
            when (action) {
                GameModeAction.LaunchFrontend -> {
                    val generation = lifecycleGeneration
                    try {
                        actionHandler.launchFrontend { success ->
                            mainHandler.post {
                                if (started && generation == lifecycleGeneration) {
                                    dispatch(stateMachine.onFrontendLaunchResult(success))
                                }
                            }
                        }
                    } catch (_: Exception) {
                        dispatch(stateMachine.onFrontendLaunchResult(false))
                    }
                }

                GameModeAction.ScheduleExitGracePeriod -> {
                    cancelExitTask()
                    val generation = lifecycleGeneration
                    val task = Runnable {
                        exitTask = null
                        if (started && generation == lifecycleGeneration) dispatch(stateMachine.onExitGracePeriodElapsed())
                    }
                    exitTask = task
                    mainHandler.postDelayed(task, exitGracePeriodMs)
                }

                GameModeAction.CancelExitGracePeriod -> cancelExitTask()

                GameModeAction.ReturnToHome -> {
                    try {
                        actionHandler.returnHome {
                            mainHandler.post {
                                if (started) dispatch(stateMachine.onReturnedToHome())
                            }
                        }
                    } catch (_: Exception) {
                        // Stay in EXITING; do not claim that HOME navigation succeeded.
                    }
                }
            }
        }
    }

    private fun cancelExitTask() {
        exitTask?.let(mainHandler::removeCallbacks)
        exitTask = null
    }

    private fun checkMainThread() {
        check(Looper.myLooper() == Looper.getMainLooper()) {
            "GameModeController must be used from the main thread"
        }
    }

    companion object {
        const val DEFAULT_EXIT_GRACE_PERIOD_MS = 3_000L
    }
}

/** Platform effects are injected so policy can be tested without launching real Activities. */
interface GameModeActionHandler {
    fun launchFrontend(onResult: (success: Boolean) -> Unit)
    fun returnHome(onComplete: () -> Unit)
}
