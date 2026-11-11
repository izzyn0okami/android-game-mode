package com.izzyn0okami.gamemode.service

import android.app.Application
import android.util.Log
import com.izzyn0okami.gamemode.ExitPromptPresenter
import com.izzyn0okami.gamemode.runtime.GameModeController
import com.izzyn0okami.gamemode.runtime.MainThreadScheduler

/**
 * Entry point of the persistent service. The system keeps this process alive
 * (android:persistent), so starting the controller here is all that is needed.
 */
class GameModeApplication : Application() {
    lateinit var exitPrompt: ExitPromptPresenter
        private set
    private var controller: GameModeController? = null

    override fun onCreate() {
        super.onCreate()
        val scheduler = MainThreadScheduler()
        val window = ExitPromptWindowAdapter(this)
        exitPrompt = ExitPromptPresenter(scheduler, window)
        controller = GameModeController(this, SystemActionHandler(this, exitPrompt)).also {
            try {
                it.start()
            } catch (failure: Throwable) {
                Log.e(TAG, "Game mode controller failed to start", failure)
            }
        }
    }

    private companion object {
        const val TAG = "GameModeService"
    }
}
