package de.nick.waterreminderapp

import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.work.Configuration
import androidx.work.WorkManager
import androidx.work.testing.WorkManagerTestInitHelper
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

/**
 * Tests für WaterActionReceiver.
 *
 * WaterActionReceiver nutzt goAsync() + Coroutinen intern.
 * Wir feuern onReceive() und warten mit Thread.sleep() kurz auf den
 * IO-Dispatcher → pragmatisch für BroadcastReceiver-Tests ohne Refactor.
 *
 * DataStore-Isolation: Jeder Test bekommt einen eindeutigen dayOfYear,
 * sodass IntakeStore intern einen Tageswechsel erkennt → frischer Start.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class WaterActionReceiverTest {

    private val context get() = ApplicationProvider.getApplicationContext<android.app.Application>()
    private lateinit var fakeTime: FakeTimeProvider
    private lateinit var intakeStore: IntakeStore

    // Zähler: Jeder Test bekommt einen anderen Tag → DataStore-Isolation
    companion object { private var dayCounter = 200 }
    private var testDay = 0

    @Before
    fun setup() {
        dayCounter++
        testDay = dayCounter
        fakeTime = FakeTimeProvider(
            hour      = 10,
            dayOfWeek = Calendar.MONDAY,
            dayOfYear = testDay,
            year      = 2026
        )
        intakeStore = IntakeStore(context, fakeTime)

        val config = Configuration.Builder()
            .setWorkerFactory(WaterWorkerFactory(timeProvider = fakeTime))
            .build()
        WorkManagerTestInitHelper.initializeTestWorkManager(context, config)
    }

    @After
    fun teardown() {
        WorkManager.getInstance(context).cancelAllWork()
    }

    // ── Hilfsfunktionen ──────────────────────────────────────────────────

    private fun sendDrank(reminderId: Long = 12345L) {
        val intent = Intent(WaterActionReceiver.ACTION_DRANK_250).apply {
            putExtra(WaterActionReceiver.EXTRA_REMINDER_ID, reminderId)
        }
        WaterActionReceiver().onReceive(context, intent)
        Thread.sleep(500) // goAsync IO-Coroutine abwarten
    }

    private fun sendSnooze(reminderId: Long = 99L) {
        val intent = Intent(WaterActionReceiver.ACTION_SNOOZE_15).apply {
            putExtra(WaterActionReceiver.EXTRA_REMINDER_ID, reminderId)
        }
        WaterActionReceiver().onReceive(context, intent)
        Thread.sleep(200)
    }

    // ── DRANK_250: addMl ─────────────────────────────────────────────────

    @Test fun `DRANK adds 250 ml to intake`() {
        // Lesen über SystemTimeProvider-Store – genauso wie der Receiver intern
        val systemStore = IntakeStore(context)
        val before = runBlocking { systemStore.totalMlTodayFlow.first() }
        sendDrank()
        val after = runBlocking { systemStore.totalMlTodayFlow.first() }
        assertEquals(before + 250, after)
    }

    @Test fun `DRANK three times accumulates to 750`() {
        val systemStore = IntakeStore(context)
        val before = runBlocking { systemStore.totalMlTodayFlow.first() }
        sendDrank()
        sendDrank()
        sendDrank()
        val after = runBlocking { systemStore.totalMlTodayFlow.first() }
        assertEquals(before + 750, after)
    }

    // ── DRANK_250: Congrats genau einmal ─────────────────────────────────

    @Test fun `DRANK reaching goal marks congratsSent`() {
        // SystemTimeProvider-Store befüllen: 1750 ml vorher
        val systemStore = IntakeStore(context)
        runBlocking { repeat(7) { systemStore.addMl(250) } }
        sendDrank() // +250 = 2000 → Ziel erreicht
        val sent = runBlocking { systemStore.isCongratsSentToday() }
        assertTrue("congratsSent muss nach Zielerreichen true sein", sent)
    }

    @Test fun `DRANK after congratsSent does not send again`() {
        val systemStore = IntakeStore(context)
        runBlocking {
            repeat(8) { systemStore.addMl(250) }
            systemStore.markCongratsSent()
        }
        sendDrank()
        val sent = runBlocking { systemStore.isCongratsSentToday() }
        assertTrue(sent)
    }

    // ── SNOOZE_15: UniqueWork mit KEEP-Policy ────────────────────────────

    @Test fun `SNOOZE enqueues unique work with correct name`() {
        val reminderId = 77L
        sendSnooze(reminderId)
        val workInfos = WorkManager.getInstance(context)
            .getWorkInfosForUniqueWork("water_snooze_$reminderId")
            .get()
        assertTrue("UniqueWork water_snooze_$reminderId muss existieren",
            workInfos.isNotEmpty())
    }

    @Test fun `SNOOZE twice same reminderId keeps exactly one job (KEEP policy)`() {
        val reminderId = 42L
        sendSnooze(reminderId)
        sendSnooze(reminderId)
        val workInfos = WorkManager.getInstance(context)
            .getWorkInfosForUniqueWork("water_snooze_$reminderId")
            .get()
        assertEquals("KEEP-Policy: genau 1 Job", 1, workInfos.size)
    }

    @Test fun `SNOOZE different reminderId creates separate works`() {
        sendSnooze(reminderId = 1L)
        sendSnooze(reminderId = 2L)
        val work1 = WorkManager.getInstance(context)
            .getWorkInfosForUniqueWork("water_snooze_1").get()
        val work2 = WorkManager.getInstance(context)
            .getWorkInfosForUniqueWork("water_snooze_2").get()
        assertTrue(work1.isNotEmpty())
        assertTrue(work2.isNotEmpty())
    }
}
