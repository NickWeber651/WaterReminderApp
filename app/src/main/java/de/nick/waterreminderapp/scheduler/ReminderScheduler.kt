package de.nick.waterreminderapp.scheduler

import android.content.Context
import android.util.Log
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import de.nick.waterreminderapp.worker.WaterReminderWorker
import java.util.concurrent.TimeUnit

object ReminderScheduler {

    private const val TAG = "ReminderScheduler"

    /**
     * Plant einen OneTimeWorkRequest mit dem gewünschten Intervall als Delay.
     *
     * Der Worker plant sich am Ende seines Laufs selbst neu ein (Ketten-Muster).
     * Dadurch entfällt der PeriodicWork-Modus komplett und es gibt keine
     * doppelten Work-Items (Periodic + OneTime) mehr.
     *
     * ExistingWorkPolicy.REPLACE stellt sicher, dass nur ein einziger
     * ausstehender Work zur gleichen Zeit existiert.
     */
    fun start(context: Context, intervalMinutes: Long = 60) {
        val wm = WorkManager.getInstance(context)
        Log.d(TAG, "▶ start() – intervalMinutes=$intervalMinutes")

        val request = OneTimeWorkRequestBuilder<WaterReminderWorker>()
            .setInitialDelay(intervalMinutes, TimeUnit.MINUTES)
            .setInputData(workDataOf(
                WaterReminderWorker.KEY_SOURCE           to "scheduled",
                WaterReminderWorker.KEY_INTERVAL_MINUTES to intervalMinutes
            ))
            .build()

        wm.enqueueUniqueWork(
            WaterReminderWorker.WORK_NAME_ONE_TIME, ExistingWorkPolicy.REPLACE, request
        )
        Log.d(TAG, "📋 OneTimeWork enqueued (REPLACE) – delay=${intervalMinutes}min")
    }

    fun stop(context: Context) {
        Log.d(TAG, "⏹ stop()")
        val wm = WorkManager.getInstance(context)
        wm.cancelUniqueWork(WaterReminderWorker.WORK_NAME_ONE_TIME)
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
