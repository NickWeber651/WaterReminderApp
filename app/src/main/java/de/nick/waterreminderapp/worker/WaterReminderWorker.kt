package de.nick.waterreminderapp.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import de.nick.waterreminderapp.data.IntakeStore
import de.nick.waterreminderapp.data.SettingsStore
import de.nick.waterreminderapp.notification.NotificationHelper
import de.nick.waterreminderapp.notification.NotificationSender
import de.nick.waterreminderapp.notification.RealNotificationSender
import de.nick.waterreminderapp.util.SystemTimeProvider
import de.nick.waterreminderapp.util.TimeProvider
import de.nick.waterreminderapp.util.TimeWindowChecker
import kotlinx.coroutines.flow.first

class WaterReminderWorker(
    context: Context,
    params: WorkerParameters,
    private val notificationSender: NotificationSender =
        RealNotificationSender(NotificationHelper(context)),
    private val timeProvider: TimeProvider = SystemTimeProvider
) : CoroutineWorker(context, params) {

    companion object {
        const val KEY_SOURCE          = "source"
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
        if (!inTimeWindow) return Result.success()

        val intakeStore = IntakeStore(applicationContext, timeProvider)
        if (intakeStore.hasReachedGoal(settings.goalMl)) return Result.success()

        val allowSnooze = inputData.getString(KEY_SOURCE) != SOURCE_SNOOZE
        notificationSender.sendReminder(System.currentTimeMillis(), allowSnooze)
        return Result.success()
    }
}

