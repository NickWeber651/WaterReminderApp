package de.nick.waterreminderapp.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import de.nick.waterreminderapp.data.DataStoreIntakeRepository
import de.nick.waterreminderapp.data.IntakeStore
import de.nick.waterreminderapp.data.SettingsStore
import de.nick.waterreminderapp.notification.NotificationHelper
import de.nick.waterreminderapp.worker.WaterReminderWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class WaterActionReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_DRANK_250  = "de.nick.waterreminderapp.ACTION_DRANK_250"
        const val ACTION_SNOOZE_15  = "de.nick.waterreminderapp.ACTION_SNOOZE_15"
        const val EXTRA_REMINDER_ID = "extra_reminder_id"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getLongExtra(EXTRA_REMINDER_ID, -1L)
        when (intent.action) {
            ACTION_DRANK_250 -> handleDrank(context)
            ACTION_SNOOZE_15 -> handleSnooze(context, reminderId)
        }
    }

    private fun handleDrank(context: Context) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repository         = DataStoreIntakeRepository.create(context)
                val intakeStore        = IntakeStore(context)   // nur noch für congratsSent
                val notificationHelper = NotificationHelper(context)

                repository.addEntry(250)

                val settings     = SettingsStore(context).settingsFlow.first()
                val totalMl      = repository.totalMlTodayFlow.first()
                val goalReached  = totalMl >= settings.goalMl
                val congratsSent = intakeStore.isCongratsSentToday()

                if (goalReached && !congratsSent) {
                    notificationHelper.showCongratsNotification()
                    intakeStore.markCongratsSent()
                }

                NotificationManagerCompat.from(context)
                    .cancel(NotificationHelper.NOTIFICATION_ID_REMINDER)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun handleSnooze(context: Context, reminderId: Long) {
        WorkManager.getInstance(context).enqueueUniqueWork(
            "water_snooze_$reminderId",
            ExistingWorkPolicy.KEEP,
            OneTimeWorkRequestBuilder<WaterReminderWorker>()
                .setInitialDelay(15, TimeUnit.MINUTES)
                .setInputData(workDataOf(WaterReminderWorker.KEY_SOURCE to "snooze"))
                .build()
        )
        NotificationManagerCompat.from(context)
            .cancel(NotificationHelper.NOTIFICATION_ID_REMINDER)
    }
}

