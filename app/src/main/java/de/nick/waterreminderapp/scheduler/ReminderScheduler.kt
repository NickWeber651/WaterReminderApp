package de.nick.waterreminderapp.scheduler

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import de.nick.waterreminderapp.worker.WaterReminderWorker
import java.util.concurrent.TimeUnit

object ReminderScheduler {

    private const val WORK_NAME = "water_reminder_periodic"

    fun start(context: Context, intervalMinutes: Long = 60) {
        val safeInterval = intervalMinutes.coerceAtLeast(1)
        val workRequest  = PeriodicWorkRequestBuilder<WaterReminderWorker>(safeInterval, TimeUnit.MINUTES)
            .setInputData(workDataOf(WaterReminderWorker.KEY_SOURCE to "scheduled"))
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, workRequest
        )
    }

    fun stop(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }
}

/**
 * Context-gebundene IReminderScheduler-Implementierung.
 * Wird im ViewModel und in der App verwendet – übergib einfach den Context.
 */
class ContextReminderScheduler(private val context: Context) : IReminderScheduler {
    override fun start(intervalMinutes: Long) = ReminderScheduler.start(context, intervalMinutes)
    override fun stop()                       = ReminderScheduler.stop(context)
}

