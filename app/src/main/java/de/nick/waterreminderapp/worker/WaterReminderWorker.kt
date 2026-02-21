package de.nick.waterreminderapp.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import de.nick.waterreminderapp.data.IntakeStore
import de.nick.waterreminderapp.data.SettingsStore
import de.nick.waterreminderapp.notification.NotificationHelper
import de.nick.waterreminderapp.notification.NotificationSender
import de.nick.waterreminderapp.notification.RealNotificationSender
import de.nick.waterreminderapp.util.SystemTimeProvider
import de.nick.waterreminderapp.util.TimeProvider
import de.nick.waterreminderapp.util.TimeWindowChecker
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

class WaterReminderWorker(
    context: Context,
    params: WorkerParameters,
    private val notificationSender: NotificationSender =
        RealNotificationSender(NotificationHelper(context)),
    private val timeProvider: TimeProvider = SystemTimeProvider
) : CoroutineWorker(context, params) {

    companion object {
        const val KEY_SOURCE            = "source"
        const val KEY_INTERVAL_MINUTES  = "interval_minutes"
        const val WORK_NAME_ONE_TIME    = "water_reminder_one_time"
        private const val SOURCE_SNOOZE = "snooze"
    }

    override suspend fun doWork(): Result {
        val settings = SettingsStore(applicationContext).settingsFlow.first()

        val inTimeWindow = TimeWindowChecker.isAllowedNow(
            weekdayStartHour = settings.weekdayStartHour,
            weekendStartHour = settings.weekendStartHour,
            endHour          = settings.endHour,
            timeProvider     = timeProvider
        )
        if (!inTimeWindow) {
            rescheduleIfNeeded()
            return Result.success()
        }

        val intakeStore = IntakeStore(applicationContext, timeProvider)
        if (!intakeStore.hasReachedGoal(settings.goalMl)) {
            val allowSnooze = inputData.getString(KEY_SOURCE) != SOURCE_SNOOZE
            notificationSender.sendReminder(System.currentTimeMillis(), allowSnooze)
        }

        rescheduleIfNeeded()
        return Result.success()
    }

    /**
     * Wenn dieser Worker als OneTimeWorkRequest läuft (intervalMinutes < 15),
     * plant er sich selbst mit dem gespeicherten Intervall neu ein.
     * Bei PeriodicWorkRequest ist intervalMinutes == -1 → kein Reschedule nötig.
     */
    private fun rescheduleIfNeeded() {
        val intervalMinutes = inputData.getLong(KEY_INTERVAL_MINUTES, -1L)
        if (intervalMinutes < 1L) return   // PeriodicWorkRequest-Modus → nichts tun

        val next = OneTimeWorkRequestBuilder<WaterReminderWorker>()
            .setInitialDelay(intervalMinutes, TimeUnit.MINUTES)
            .setInputData(workDataOf(
                KEY_SOURCE           to inputData.getString(KEY_SOURCE),
                KEY_INTERVAL_MINUTES to intervalMinutes
            ))
            .build()

        WorkManager.getInstance(applicationContext)
            .enqueueUniqueWork(WORK_NAME_ONE_TIME, ExistingWorkPolicy.REPLACE, next)
    }
}
