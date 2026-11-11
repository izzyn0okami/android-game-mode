package com.izzyn0okami.gamemode

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ExitPromptPresenterTest {
    @Test
    fun showOpensTheWindowAndWaitsForTheAnswer() {
        val fixture = Fixture()
        fixture.presenter.show(fixture::record)

        assertEquals(1, fixture.window.opened)
        assertTrue(fixture.results.isEmpty())
    }

    @Test
    fun answerIsDeliveredOnceAndClosesTheWindow() {
        val fixture = Fixture()
        fixture.presenter.show(fixture::record)
        assertTrue(fixture.presenter.onWindowShown())

        fixture.presenter.onWindowAnswered(ExitPromptResult.RETURN_HOME)
        fixture.presenter.onWindowAnswered(ExitPromptResult.STAY)

        assertEquals(listOf(ExitPromptResult.RETURN_HOME), fixture.results)
        assertEquals(1, fixture.window.closed)
    }

    @Test
    fun windowThatNeverAppearsAnswersStayAfterTheTimeout() {
        val fixture = Fixture(shownTimeoutMs = 2_000)
        fixture.presenter.show(fixture::record)

        fixture.scheduler.advanceBy(1_999)
        assertTrue(fixture.results.isEmpty())
        fixture.scheduler.advanceBy(1)

        assertEquals(listOf(ExitPromptResult.STAY), fixture.results)
        assertEquals(1, fixture.window.closed)
    }

    @Test
    fun shownWindowWaitsForTheUserWithoutAnyTimeout() {
        val fixture = Fixture()
        fixture.presenter.show(fixture::record)
        assertTrue(fixture.presenter.onWindowShown())

        fixture.scheduler.advanceBy(3_600_000)

        assertTrue(fixture.results.isEmpty())
        assertEquals(0, fixture.window.closed)
    }

    @Test
    fun lateAnswerAfterTheTimeoutIsIgnored() {
        val fixture = Fixture(shownTimeoutMs = 2_000)
        fixture.presenter.show(fixture::record)
        fixture.scheduler.advanceBy(2_000)

        fixture.presenter.onWindowAnswered(ExitPromptResult.RETURN_HOME)

        assertEquals(listOf(ExitPromptResult.STAY), fixture.results)
    }

    @Test
    fun dismissClosesTheWindowWithoutAnswering() {
        val fixture = Fixture()
        fixture.presenter.show(fixture::record)
        fixture.presenter.onWindowShown()

        fixture.presenter.dismiss()
        fixture.presenter.onWindowClosedWithoutAnswer()
        fixture.scheduler.advanceBy(60_000)

        assertEquals(1, fixture.window.closed)
        assertTrue(fixture.results.isEmpty())
    }

    @Test
    fun dismissCancelsTheShownTimeout() {
        val fixture = Fixture()
        fixture.presenter.show(fixture::record)

        fixture.presenter.dismiss()
        fixture.scheduler.advanceBy(60_000)

        assertTrue(fixture.results.isEmpty())
    }

    @Test
    fun dismissWithoutAPendingPromptDoesNothing() {
        val fixture = Fixture()
        fixture.presenter.dismiss()

        assertEquals(0, fixture.window.closed)
    }

    @Test
    fun windowShownAfterDismissIsNotWanted() {
        val fixture = Fixture()
        fixture.presenter.show(fixture::record)
        fixture.presenter.dismiss()

        assertFalse(fixture.presenter.onWindowShown())
    }

    @Test
    fun windowClosedWithoutAnswerCountsAsStay() {
        val fixture = Fixture()
        fixture.presenter.show(fixture::record)
        fixture.presenter.onWindowShown()

        fixture.presenter.onWindowClosedWithoutAnswer()

        assertEquals(listOf(ExitPromptResult.STAY), fixture.results)
    }

    @Test
    fun openFailureAnswersStayWithoutWaiting() {
        val fixture = Fixture()
        fixture.window.failOpen = true

        fixture.presenter.show(fixture::record)
        fixture.scheduler.advanceBy(60_000)

        assertEquals(listOf(ExitPromptResult.STAY), fixture.results)
    }

    @Test
    fun showReplacesAPendingPromptWithoutAnsweringTheOldOne() {
        val fixture = Fixture()
        val first = mutableListOf<ExitPromptResult>()
        fixture.presenter.show { first += it }
        fixture.presenter.onWindowShown()

        fixture.presenter.show(fixture::record)
        assertEquals(2, fixture.window.opened)
        assertTrue(fixture.presenter.onWindowShown())
        fixture.presenter.onWindowAnswered(ExitPromptResult.RETURN_HOME)

        assertTrue(first.isEmpty())
        assertEquals(listOf(ExitPromptResult.RETURN_HOME), fixture.results)
    }

    @Test
    fun rejectsANonPositiveShownTimeout() {
        assertFailsWith<IllegalArgumentException> {
            ExitPromptPresenter(FakeScheduler(), FakeWindow(), shownTimeoutMs = 0)
        }
    }

    private class Fixture(shownTimeoutMs: Long = 2_000) {
        val scheduler = FakeScheduler()
        val window = FakeWindow()
        val presenter = ExitPromptPresenter(scheduler, window, shownTimeoutMs)
        val results = mutableListOf<ExitPromptResult>()

        fun record(result: ExitPromptResult) {
            results += result
        }
    }

    private class FakeWindow : ExitPromptWindow {
        var opened = 0
        var closed = 0
        var failOpen = false

        override fun open() {
            if (failOpen) throw IllegalStateException("no window")
            opened++
        }

        override fun close() {
            closed++
        }
    }

    private class FakeScheduler : GameModeScheduler {
        private class Task(val at: Long, val run: () -> Unit, var done: Boolean = false)

        private val tasks = mutableListOf<Task>()
        private var now = 0L

        override fun post(task: () -> Unit) = task()

        override fun postDelayed(delayMs: Long, task: () -> Unit): GameModeCancellation {
            val entry = Task(now + delayMs, task)
            tasks += entry
            return GameModeCancellation { entry.done = true }
        }

        fun advanceBy(ms: Long) {
            now += ms
            tasks.filter { !it.done && it.at <= now }.sortedBy { it.at }.forEach { entry ->
                if (!entry.done) {
                    entry.done = true
                    entry.run()
                }
            }
        }
    }
}
