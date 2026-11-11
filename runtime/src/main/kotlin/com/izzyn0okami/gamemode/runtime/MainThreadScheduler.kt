package com.izzyn0okami.gamemode.runtime

import android.os.Handler
import android.os.Looper
import com.izzyn0okami.gamemode.GameModeCancellation
import com.izzyn0okami.gamemode.GameModeScheduler

/** Runs tasks on the main thread, which is where every game mode component is serialized. */
class MainThreadScheduler(
    private val handler: Handler = Handler(Looper.getMainLooper()),
) : GameModeScheduler {
    override fun post(task: () -> Unit) {
        handler.post(Runnable(task))
    }

    override fun postDelayed(delayMs: Long, task: () -> Unit): GameModeCancellation {
        val runnable = Runnable(task)
        handler.postDelayed(runnable, delayMs)
        return GameModeCancellation { handler.removeCallbacks(runnable) }
    }
}
