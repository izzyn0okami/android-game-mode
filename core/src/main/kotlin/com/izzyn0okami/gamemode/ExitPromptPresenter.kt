package com.izzyn0okami.gamemode

/** The window that asks the user whether to leave game mode; the Android layer implements it. */
interface ExitPromptWindow {
    fun open()
    fun close()
}

/**
 * Tracks the single pending exit prompt between the coordinator and the window that
 * presents it. The window reports back through [onWindowShown], [onWindowAnswered] and
 * [onWindowClosedWithoutAnswer].
 *
 * The user's answer has no timeout, but the window must appear within [shownTimeoutMs]:
 * a window that never shows (for example a blocked background launch) is treated as
 * [ExitPromptResult.STAY] so the state machine is never left waiting for a prompt nobody
 * can see. All calls must come from the same thread as the scheduler.
 */
class ExitPromptPresenter(
    private val scheduler: GameModeScheduler,
    private val window: ExitPromptWindow,
    private val shownTimeoutMs: Long = DEFAULT_SHOWN_TIMEOUT_MS,
) {
    private var pending: ((ExitPromptResult) -> Unit)? = null
    private var shown = false
    private var timeout: GameModeCancellation? = null

    init {
        require(shownTimeoutMs > 0) { "Shown timeout must be positive" }
    }

    /** Opens the window and delivers the answer to [onResult] at most once. */
    fun show(onResult: (ExitPromptResult) -> Unit) {
        dismiss()
        pending = onResult
        try {
            window.open()
        } catch (_: Exception) {
            answer(ExitPromptResult.STAY)
            return
        }
        if (pending !== onResult || shown) return
        timeout = scheduler.postDelayed(shownTimeoutMs) {
            timeout = null
            if (pending === onResult && !shown) answer(ExitPromptResult.STAY)
        }
    }

    /** Closes the window and drops the pending prompt without answering it. */
    fun dismiss() {
        if (pending == null) return
        clear()
        closeWindow()
    }

    /** Returns false when no prompt is pending, so a late window should finish itself. */
    fun onWindowShown(): Boolean {
        if (pending == null) return false
        shown = true
        timeout?.cancel()
        timeout = null
        return true
    }

    fun onWindowAnswered(result: ExitPromptResult) = answer(result)

    fun onWindowClosedWithoutAnswer() = answer(ExitPromptResult.STAY)

    private fun answer(result: ExitPromptResult) {
        val callback = pending ?: return
        clear()
        closeWindow()
        callback(result)
    }

    private fun clear() {
        pending = null
        shown = false
        timeout?.cancel()
        timeout = null
    }

    private fun closeWindow() {
        try {
            window.close()
        } catch (_: Exception) {
            // The window owner is responsible for its own teardown.
        }
    }

    companion object {
        const val DEFAULT_SHOWN_TIMEOUT_MS = 3_000L
    }
}
