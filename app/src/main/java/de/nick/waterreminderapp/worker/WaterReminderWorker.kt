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
        val intervalMinutes = settings.intervalMinutes.toLong()

        val inTimeWindow = TimeWindowChecker.isAllowedNow(
            weekdayStartHour = settings.weekdayStartHour,
            weekendStartHour = settings.weekendStartHour,
            endHour          = settings.endHour,
            timeProvider     = timeProvider
        )
        if (!inTimeWindow) {
            Log.d(TAG, "⏭ Außerhalb Zeitfenster → skip (hour=${timeProvider.currentHour()})")
            // Snooze-Worker ist einmalig – darf den regulären Timer nicht überschreiben
            if (source != SOURCE_SNOOZE) {
                rescheduleToNextWindow(settings)
            } else {
                Log.d(TAG, "🔄 Snooze außerhalb Zeitfenster → kein Reschedule")
            }
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

        // Snooze-Worker ist ein einmaliger Reminder – darf den regulären Timer nicht überschreiben
        if (source != SOURCE_SNOOZE) {
            rescheduleIfNeeded(intervalMinutes)
        } else {
            Log.d(TAG, "🔄 Snooze-Modus → kein Reschedule (regulärer Timer läuft weiter)")
        }
        Log.d(TAG, "✅ doWork() ENDE")
        return Result.success()
    }

    /**
     * Plant den nächsten Lauf genau zum Start des nächsten Zeitfensters ein.
     * Wird aufgerufen, wenn der Worker außerhalb des Zeitfensters feuert,
     * damit der Reminder pünktlich zum Fenster-Start kommt (z.B. 8:00 Uhr).
     */
    private fun rescheduleToNextWindow(settings: de.nick.waterreminderapp.data.Settings) {
        val delayMinutes = TimeWindowChecker.minutesUntilNextWindow(
            weekdayStartHour = settings.weekdayStartHour,
            weekendStartHour = settings.weekendStartHour,
            endHour          = settings.endHour,
            timeProvider     = timeProvider
        )
        Log.d(TAG, "⏰ Reschedule zum nächsten Zeitfenster in $delayMinutes min")

        val next = OneTimeWorkRequestBuilder<WaterReminderWorker>()
            .setInitialDelay(delayMinutes, TimeUnit.MINUTES)
            .setInputData(workDataOf(
                KEY_SOURCE           to "scheduled",
                KEY_INTERVAL_MINUTES to settings.intervalMinutes.toLong()
            ))
            .build()

        WorkManager.getInstance(applicationContext)
            .enqueueUniqueWork(WORK_NAME_ONE_TIME, ExistingWorkPolicy.REPLACE, next)
    }

    /**
     * Plant den nächsten Lauf nach dem konfigurierten Intervall ein.
     * Da wir nur noch OneTimeWork verwenden (kein PeriodicWork mehr),
     * muss sich der Worker nach jedem Lauf selbst neu einplanen.
     */
    private fun rescheduleIfNeeded(intervalMinutes: Long) {
        if (intervalMinutes < 1L) {
            Log.d(TAG, "🔄 Intervall ungültig ($intervalMinutes) → kein Reschedule")
            return
        }

        Log.d(TAG, "🔄 Reschedule in $intervalMinutes min")
        val next = OneTimeWorkRequestBuilder<WaterReminderWorker>()
            .setInitialDelay(intervalMinutes, TimeUnit.MINUTES)
            .setInputData(workDataOf(
                KEY_SOURCE           to "scheduled",
                KEY_INTERVAL_MINUTES to intervalMinutes
            ))
            .build()

        WorkManager.getInstance(applicationContext)
            .enqueueUniqueWork(WORK_NAME_ONE_TIME, ExistingWorkPolicy.REPLACE, next)
    }
}
