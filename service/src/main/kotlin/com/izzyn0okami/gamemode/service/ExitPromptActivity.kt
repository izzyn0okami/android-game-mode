package com.izzyn0okami.gamemode.service

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.izzyn0okami.gamemode.ExitPromptResult
import com.izzyn0okami.gamemode.ExitPromptWindow
import java.lang.ref.WeakReference

/** Asks whether to leave game mode. Buttons are focusable, so a controller can answer. */
class ExitPromptActivity : Activity() {
    private var answered = false
    private var dialog: AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val presenter = (application as GameModeApplication).exitPrompt
        if (!presenter.onWindowShown()) {
            finish()
            return
        }
        current = WeakReference(this)
        dialog = AlertDialog.Builder(this)
            .setTitle(R.string.exit_prompt_title)
            .setMessage(R.string.exit_prompt_message)
            .setCancelable(false)
            .setPositiveButton(R.string.exit_prompt_return_home) { _, _ -> answer(ExitPromptResult.RETURN_HOME) }
            .setNegativeButton(R.string.exit_prompt_stay) { _, _ -> answer(ExitPromptResult.STAY) }
            .show()
    }

    private fun answer(result: ExitPromptResult) {
        answered = true
        (application as GameModeApplication).exitPrompt.onWindowAnswered(result)
        finish()
    }

    override fun onDestroy() {
        dialog?.dismiss()
        if (current?.get() === this) current = null
        // Destroyed without an answer or a dismiss from the presenter (for example killed
        // by the system): treat as STAY. A no-op when the presenter already closed us.
        if (!answered) (application as GameModeApplication).exitPrompt.onWindowClosedWithoutAnswer()
        super.onDestroy()
    }

    companion object {
        private var current: WeakReference<ExitPromptActivity>? = null

        fun finishCurrent() {
            current?.get()?.let {
                it.answered = true
                it.finish()
            }
        }
    }
}

/** Opens [ExitPromptActivity] from the service process and closes it on request. */
class ExitPromptWindowAdapter(private val context: Context) : ExitPromptWindow {
    override fun open() {
        context.startActivity(
            Intent(context, ExitPromptActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }

    override fun close() = ExitPromptActivity.finishCurrent()
}
