package de.nick.waterreminderapp

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.Configuration
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.testing.TestDriver
import androidx.work.testing.WorkManagerTestInitHelper
import androidx.work.workDataOf
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
class WaterReminderWorkerTest {

    private lateinit var context: Context
    private lateinit var fakeSender: FakeNotificationSender
    private lateinit var fakeTime: FakeTimeProvider
    private lateinit var workManager: WorkManager
    private lateinit var testDriver: TestDriver

    companion object { private var testDayOfYear = 100 }
    private var currentDay = 0

    @Before
    fun setup() {
        context    = ApplicationProvider.getApplicationContext()
        fakeSender = FakeNotificationSender()

        // Eindeutiger Tag pro Test → kein DataStore-State aus vorherigem Test
        testDayOfYear++
        currentDay = testDayOfYear
        fakeTime = FakeTimeProvider(
            hour      = 10,
            dayOfWeek = Calendar.MONDAY,
            dayOfYear = currentDay,
            year      = 2026
        )

        val config = Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.DEBUG)
            .setWorkerFactory(WaterWorkerFactory(fakeSender, fakeTime))
            .build()

        WorkManagerTestInitHelper.initializeTestWorkManager(context, config)
        workManager = WorkManager.getInstance(context)
        testDriver  = WorkManagerTestInitHelper.getTestDriver(context)!!
    }

    @After
    fun teardown() {
        fakeSender.reset()
    }

    // Hilfsfunktion: Worker enqueuen + synchron ausführen via TestDriver
    private fun runWorker(source: String = "scheduled"): WorkInfo {
        val request = OneTimeWorkRequestBuilder<WaterReminderWorker>()
            .setInputData(workDataOf(WaterReminderWorker.KEY_SOURCE to source))
            .build()
        workManager.enqueue(request)
        testDriver.setAllConstraintsMet(request.id)
        return workManager.getWorkInfoById(request.id).get()!!
    }

    // ── Zeitfenster ──────────────────────────────────────────────────────

    @Test fun `outside time window end hour - no notification`() {
        fakeTime.setHour(23) // endHour=23 ist exklusiv
        runWorker()
        assertTrue(fakeSender.reminderCalls.isEmpty())
    }

    @Test fun `before weekday start hour - no notification`() {
        fakeTime.setHour(7) // weekdayStart=8
        runWorker()
        assertTrue(fakeSender.reminderCalls.isEmpty())
    }

    @Test fun `inside weekday window - notification sent`() {
        fakeTime.setHour(10)
        runWorker()
        assertEquals(1, fakeSender.reminderCalls.size)
    }

    @Test fun `saturday before weekend start - no notification`() {
        fakeTime.setDayOfWeek(Calendar.SATURDAY)
        fakeTime.setHour(8) // weekendStart=9
        runWorker()
        assertTrue(fakeSender.reminderCalls.isEmpty())
    }

    @Test fun `saturday at weekend start - notification sent`() {
        fakeTime.setDayOfWeek(Calendar.SATURDAY)
        fakeTime.setHour(9)
        runWorker()
        assertEquals(1, fakeSender.reminderCalls.size)
    }

    // ── Tagesziel erreicht ───────────────────────────────────────────────

    @Test fun `goal already reached - no notification`() {
        fakeTime.setHour(10)
        runBlocking {
            val store = IntakeStore(context, fakeTime)
            repeat(8) { store.addMl(250) }
        }
        runWorker()
        assertTrue("Keine Notification wenn Ziel erreicht",
            fakeSender.reminderCalls.isEmpty())
    }

    // ── source-Flag → allowSnooze ────────────────────────────────────────

    @Test fun `source snooze sets allowSnooze false`() {
        fakeTime.setHour(10)
        runWorker(source = "snooze")
        assertEquals(1, fakeSender.reminderCalls.size)
        assertEquals(false, fakeSender.reminderCalls[0].allowSnooze)
    }

    @Test fun `source scheduled sets allowSnooze true`() {
        fakeTime.setHour(10)
        runWorker(source = "scheduled")
        assertEquals(1, fakeSender.reminderCalls.size)
        assertEquals(true, fakeSender.reminderCalls[0].allowSnooze)
    }

    @Test fun `worker always returns SUCCEEDED`() {
        fakeTime.setHour(10)
        val info = runWorker()
        assertEquals(WorkInfo.State.SUCCEEDED, info.state)
    }
}
