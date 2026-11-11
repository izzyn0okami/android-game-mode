package com.izzyn0okami.gamemode.service

import android.content.Context
import android.content.Intent
import android.util.Log
import com.izzyn0okami.gamemode.ExitPromptPresenter
import com.izzyn0okami.gamemode.ExitPromptResult
import com.izzyn0okami.gamemode.runtime.GameModeActionHandler

/**
 * Opens the frontend and returns to HOME. Background activity launches are allowed by the
 * privileged START_ACTIVITIES_FROM_BACKGROUND permission; when the system blocks one,
 * startActivity does not throw, so success here only means the request was accepted.
 */
class SystemActionHandler(
    private val context: Context,
    private val exitPrompt: ExitPromptPresenter,
) : GameModeActionHandler {

    override fun launchFrontend(onResult: (success: Boolean) -> Unit) {
        val intent = context.packageManager.getLaunchIntentForPackage(BuildConfig.FRONTEND_PACKAGE)
        if (intent == null) {
            Log.w(TAG, "Frontend ${BuildConfig.FRONTEND_PACKAGE} not found")
            onResult(false)
            return
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        onResult(start(intent))
    }

    override fun returnHome(onComplete: () -> Unit) {
        val home = Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_HOME)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        start(home)
        onComplete()
    }

    override fun showExitPrompt(onResult: (ExitPromptResult) -> Unit) = exitPrompt.show(onResult)

    override fun dismissExitPrompt() = exitPrompt.dismiss()

    private fun start(intent: Intent): Boolean = try {
        context.startActivity(intent)
        true
    } catch (failure: Exception) {
        Log.e(TAG, "startActivity failed", failure)
        false
    }

    private companion object {
        const val TAG = "GameModeService"
    }
}
