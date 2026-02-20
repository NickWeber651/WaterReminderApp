package de.nick.waterreminderapp.receiver

import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.work.Configuration
import androidx.work.WorkManager
import androidx.work.testing.WorkManagerTestInitHelper
import de.nick.waterreminderapp.data.IntakeStore
import de.nick.waterreminderapp.util.FakeTimeProvider
import de.nick.waterreminderapp.worker.WaterWorkerFactory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Calendar

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class WaterActionReceiverTest {

    private val context get() = ApplicationProvider.getApplicationContext<android.app.Application>()

    companion object { private var dayCounter = 200 }
    private var testDay = 0
    private lateinit var fakeTime: FakeTimeProvider

    @Before
    fun setup() {
        dayCounter++; testDay = dayCounter
        fakeTime = FakeTimeProvider(10, Calendar.MONDAY, testDay, 2026)
        WorkManagerTestInitHelper.initializeTestWorkManager(context,
            Configuration.Builder().setWorkerFactory(WaterWorkerFactory(timeProvider = fakeTime)).build())
    }

    @After fun teardown() { WorkManager.getInstance(context).cancelAllWork() }

    private fun sendDrank() {
        WaterActionReceiver().onReceive(context,
            Intent(WaterActionReceiver.ACTION_DRANK_250).apply {
                putExtra(WaterActionReceiver.EXTRA_REMINDER_ID, 12345L) })
        Thread.sleep(500)
    }

    private fun sendSnooze(reminderId: Long = 99L) {
        WaterActionReceiver().onReceive(context,
            Intent(WaterActionReceiver.ACTION_SNOOZE_15).apply {
                putExtra(WaterActionReceiver.EXTRA_REMINDER_ID, reminderId) })
        Thread.sleep(200)
    }

    @Test fun `DRANK erhoeht Intake um 250 ml`() {
        val store  = IntakeStore(context)
        val before = runBlocking { store.totalMlTodayFlow.first() }
        sendDrank()
        assertEquals(before + 250, runBlocking { store.totalMlTodayFlow.first() })
    }

    @Test fun `DRANK dreimal ergibt 750 ml mehr`() {
        val store  = IntakeStore(context)
        val before = runBlocking { store.totalMlTodayFlow.first() }
        sendDrank(); sendDrank(); sendDrank()
        assertEquals(before + 750, runBlocking { store.totalMlTodayFlow.first() })
    }

    @Test fun `DRANK bei Zielerreichen setzt congratsSent`() {
        val store = IntakeStore(context)
        runBlocking { repeat(7) { store.addMl(250) } }
        sendDrank()
        assertTrue(runBlocking { store.isCongratsSentToday() })
    }

    @Test fun `DRANK nach congratsSent aendert Status nicht`() {
        val store = IntakeStore(context)
        runBlocking { repeat(8) { store.addMl(250) }; store.markCongratsSent() }
        sendDrank()
        assertTrue(runBlocking { store.isCongratsSentToday() })
    }

    @Test fun `SNOOZE erstellt UniqueWork mit korrektem Namen`() {
        sendSnooze(77L)
        assertTrue(WorkManager.getInstance(context)
            .getWorkInfosForUniqueWork("water_snooze_77").get().isNotEmpty())
    }

    @Test fun `SNOOZE zweimal gleiche ID behaelt genau einen Job`() {
        sendSnooze(42L); sendSnooze(42L)
        assertEquals(1, WorkManager.getInstance(context)
            .getWorkInfosForUniqueWork("water_snooze_42").get().size)
    }

    @Test fun `SNOOZE verschiedene IDs erstellen separate Jobs`() {
        sendSnooze(1L); sendSnooze(2L)
        assertTrue(WorkManager.getInstance(context).getWorkInfosForUniqueWork("water_snooze_1").get().isNotEmpty())
        assertTrue(WorkManager.getInstance(context).getWorkInfosForUniqueWork("water_snooze_2").get().isNotEmpty())
    }
}

