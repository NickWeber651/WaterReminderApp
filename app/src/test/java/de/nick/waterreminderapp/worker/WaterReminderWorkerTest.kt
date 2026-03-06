package de.nick.waterreminderapp.worker

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.Configuration
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.testing.WorkManagerTestInitHelper
import androidx.work.workDataOf
import de.nick.waterreminderapp.data.DataStoreIntakeRepository
import de.nick.waterreminderapp.notification.FakeNotificationSender
import de.nick.waterreminderapp.util.FakeTimeProvider
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
import java.util.concurrent.Executors

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class WaterReminderWorkerTest {

    private lateinit var context: Context
    private lateinit var fakeSender: FakeNotificationSender
    private lateinit var fakeTime: FakeTimeProvider
    private lateinit var workManager: WorkManager

    // Jeder Test bekommt einen eigenen Tag → DataStore-Isolation
    // AtomicInteger wäre sauberer, aber companion reicht hier
    companion object {
        private var testDayOfYear = 100
    }
    private var currentDay = 0

    @Before
    fun setup() {
        context    = ApplicationProvider.getApplicationContext()
        fakeSender = FakeNotificationSender()
        testDayOfYear++
        currentDay = testDayOfYear
        fakeTime   = FakeTimeProvider(10, Calendar.MONDAY, currentDay, 2026)

        // SynchronousExecutor → Worker läuft im selben Thread, kein Race Condition
        val config = Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.DEBUG)
            .setWorkerFactory(WaterWorkerFactory(fakeSender, fakeTime))
            .setExecutor(Executors.newSingleThreadExecutor())
            .build()

        WorkManagerTestInitHelper.initializeTestWorkManager(context, config)
        workManager = WorkManager.getInstance(context)
    }

    @After fun teardown() { fakeSender.reset() }

    // Worker enqueuen und auf SUCCEEDED warten (max. 5 Sekunden)
    private fun runWorker(source: String = "scheduled"): WorkInfo {
        val request = OneTimeWorkRequestBuilder<WaterReminderWorker>()
            .setInputData(workDataOf(WaterReminderWorker.KEY_SOURCE to source))
            .build()
        workManager.enqueue(request).result.get() // blockiert bis enqueue abgeschlossen

        // Auf finalen State warten
        val deadline = System.currentTimeMillis() + 5_000
        while (System.currentTimeMillis() < deadline) {
            val info = workManager.getWorkInfoById(request.id).get()!!
            if (info.state.isFinished) return info
            Thread.sleep(50)
        }
        return workManager.getWorkInfoById(request.id).get()!!
    }

    // ── Zeitfenster ──────────────────────────────────────────────────────

    @Test fun `ausserhalb Zeitfenster - keine Notification`() {
        fakeTime.setHour(23)
        runWorker()
        assertTrue(fakeSender.reminderCalls.isEmpty())
    }

    @Test fun `vor Wochentag-Start - keine Notification`() {
        fakeTime.setHour(7)
        runWorker()
        assertTrue(fakeSender.reminderCalls.isEmpty())
    }

    @Test fun `innerhalb Zeitfenster Wochentag - Notification gesendet`() {
        fakeTime.setHour(10)
        runWorker()
        assertEquals(1, fakeSender.reminderCalls.size)
    }

    @Test fun `Samstag vor 09 Uhr - keine Notification`() {
        fakeTime.setDayOfWeek(Calendar.SATURDAY)
        fakeTime.setHour(8)
        runWorker()
        assertTrue(fakeSender.reminderCalls.isEmpty())
    }

    @Test fun `Samstag um 09 Uhr - Notification gesendet`() {
        fakeTime.setDayOfWeek(Calendar.SATURDAY)
        fakeTime.setHour(9)
        runWorker()
        assertEquals(1, fakeSender.reminderCalls.size)
    }

    // ── Tagesziel ────────────────────────────────────────────────────────

    @Test fun `Tagesziel erreicht - keine Notification`() {
        fakeTime.setHour(10)
        runBlocking {
            val repo = DataStoreIntakeRepository.create(context, fakeTime)
            repeat(8) { repo.addEntry(250) } // 8 × 250 = 2000 ml
        }
        runWorker()
        assertTrue(fakeSender.reminderCalls.isEmpty())
    }

    // ── source-Flag → allowSnooze ─────────────────────────────────────────

    @Test fun `source snooze setzt allowSnooze false`() {
        fakeTime.setHour(10)
        runWorker("snooze")
        assertEquals(1, fakeSender.reminderCalls.size)
        assertEquals(false, fakeSender.reminderCalls[0].allowSnooze)
    }

    @Test fun `source scheduled setzt allowSnooze true`() {
        fakeTime.setHour(10)
        runWorker("scheduled")
        assertEquals(1, fakeSender.reminderCalls.size)
        assertEquals(true, fakeSender.reminderCalls[0].allowSnooze)
    }

    @Test fun `worker gibt immer SUCCEEDED zurueck`() {
        fakeTime.setHour(10)
        assertEquals(WorkInfo.State.SUCCEEDED, runWorker().state)
    }
}
