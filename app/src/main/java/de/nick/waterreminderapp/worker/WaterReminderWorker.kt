package de.nick.waterreminderapp.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import de.nick.waterreminderapp.data.DataStoreIntakeRepository
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
        private const val TAG = "WaterReminderWorker"
        const val KEY_SOURCE            = "source"
        const val KEY_INTERVAL_MINUTES  = "interval_minutes"
        const val WORK_NAME_ONE_TIME    = "water_reminder_one_time"
        private const val SOURCE_SNOOZE = "snooze"
    }

    override suspend fun doWork(): Result {
        val source = inputData.getString(KEY_SOURCE) ?: "unknown"
        Log.d(TAG, "▶ doWork() START – source=$source, runAttemptCount=$runAttemptCount")

        val settings = SettingsStore(applicationContext).settingsFlow.first()

        val inTimeWindow = TimeWindowChecker.isAllowedNow(
            weekdayStartHour = settings.weekdayStartHour,
            weekendStartHour = settings.weekendStartHour,
            endHour          = settings.endHour,
            timeProvider     = timeProvider
        )
        if (!inTimeWindow) {
            Log.d(TAG, "⏭ Außerhalb Zeitfenster → skip (hour=${timeProvider.currentHour()})")
            rescheduleIfNeeded()
            return Result.success()
        }

        val repository  = DataStoreIntakeRepository.create(applicationContext, timeProvider)
        val totalMl     = repository.totalMlTodayFlow.first()
        val goalReached = totalMl >= settings.goalMl
        Log.d(TAG, "📊 goalReached=$goalReached (goal=${settings.goalMl} ml, total=$totalMl ml)")

        if (!goalReached) {
            val allowSnooze = source != SOURCE_SNOOZE
            val sent = notificationSender.sendReminder(System.currentTimeMillis(), allowSnooze)
            Log.d(TAG, "🔔 Notification gesendet=$sent, allowSnooze=$allowSnooze")
        } else {
            Log.d(TAG, "🎯 Goal bereits erreicht → kein Reminder")
        }

        rescheduleIfNeeded()
        Log.d(TAG, "✅ doWork() ENDE")
        return Result.success()
    }

    /**
     * Wenn dieser Worker als OneTimeWorkRequest läuft (intervalMinutes < 15),
     * plant er sich selbst mit dem gespeicherten Intervall neu ein.
     * Bei PeriodicWorkRequest ist intervalMinutes == -1 → kein Reschedule nötig.
     */
    private fun rescheduleIfNeeded() {
        val intervalMinutes = inputData.getLong(KEY_INTERVAL_MINUTES, -1L)
        if (intervalMinutes < 1L) {
            Log.d(TAG, "🔄 Periodic-Modus → kein manuelles Reschedule nötig")
            return
        }

        Log.d(TAG, "🔄 OneTime-Modus → Reschedule in $intervalMinutes min")
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
