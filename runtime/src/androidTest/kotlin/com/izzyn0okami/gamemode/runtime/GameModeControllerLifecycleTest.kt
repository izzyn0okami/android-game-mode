package com.izzyn0okami.gamemode.runtime

import android.content.BroadcastReceiver
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.IntentFilter
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GameModeControllerLifecycleTest {

    @Test
    fun startAndStopAreIdempotentAndCanBeRepeated() {
        val targetContext = InstrumentationRegistry.getInstrumentation().targetContext
        val context = CountingContext(targetContext)
        val actionHandler = RecordingActionHandler()
        val controller = GameModeController(context, actionHandler)

        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            controller.start()
            controller.start()
            assertEquals(1, context.registerCount)

            controller.stop()
            controller.stop()
            assertEquals(1, context.unregisterCount)

            controller.start()
            assertEquals(2, context.registerCount)

            controller.stop()
            assertEquals(2, context.unregisterCount)
        }

        assertEquals(0, actionHandler.launchCount)
        assertEquals(0, actionHandler.homeCount)
    }

    private class CountingContext(base: Context) : ContextWrapper(base) {
        var registerCount = 0
        var unregisterCount = 0

        override fun getApplicationContext(): Context = this

        override fun registerReceiver(
            receiver: BroadcastReceiver?,
            filter: IntentFilter,
            flags: Int,
        ): Intent? {
            registerCount += 1
            return super.registerReceiver(receiver, filter, flags)
        }

        override fun unregisterReceiver(receiver: BroadcastReceiver) {
            unregisterCount += 1
            super.unregisterReceiver(receiver)
        }
    }

    private class RecordingActionHandler : GameModeActionHandler {
        var launchCount = 0
        var homeCount = 0

        override fun launchFrontend(onResult: (success: Boolean) -> Unit) {
            launchCount += 1
            onResult(true)
        }

        override fun returnHome(onComplete: () -> Unit) {
            homeCount += 1
            onComplete()
        }
    }
}
