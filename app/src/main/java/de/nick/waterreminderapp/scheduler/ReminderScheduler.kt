package de.nick.waterreminderapp.scheduler

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import de.nick.waterreminderapp.worker.WaterReminderWorker
import java.util.concurrent.TimeUnit

object ReminderScheduler {

    private const val WORK_NAME_PERIODIC = "water_reminder_periodic"

    /**
     * Wählt automatisch den richtigen WorkManager-Modus:
     *
     *  >= 15 min → PeriodicWorkRequest  (batterieschonend, OS-verwaltet)
     *  <  15 min → OneTimeWorkRequest   (Worker plant sich nach jedem Lauf selbst neu ein)
     *
     * Hintergrund: WorkManager erzwingt für PeriodicWork ein Systemminimum
     * von 15 Minuten. Kleinere Intervalle sind nur via OneTimeWork möglich.
     */
    fun start(context: Context, intervalMinutes: Long = 60) {
        val wm = WorkManager.getInstance(context)

        if (intervalMinutes >= 15) {
            // Periodisch-Modus: altes OneTime-Work stoppen falls vorhanden
            wm.cancelUniqueWork(WaterReminderWorker.WORK_NAME_ONE_TIME)

            val request = PeriodicWorkRequestBuilder<WaterReminderWorker>(
                intervalMinutes, TimeUnit.MINUTES
            ).setInputData(
                workDataOf(WaterReminderWorker.KEY_SOURCE to "scheduled")
                // KEY_INTERVAL_MINUTES absichtlich nicht gesetzt → Worker weiß: Periodic-Modus
            ).build()

            wm.enqueueUniquePeriodicWork(
                WORK_NAME_PERIODIC, ExistingPeriodicWorkPolicy.UPDATE, request
            )
        } else {
            // OneTime-Modus: altes Periodic-Work stoppen falls vorhanden
            wm.cancelUniqueWork(WORK_NAME_PERIODIC)

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
        }
    }

    fun stop(context: Context) {
        val wm = WorkManager.getInstance(context)
        wm.cancelUniqueWork(WORK_NAME_PERIODIC)
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
