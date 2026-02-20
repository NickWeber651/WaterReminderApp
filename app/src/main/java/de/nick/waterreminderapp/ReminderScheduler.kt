package de.nick.waterreminderapp

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.util.concurrent.TimeUnit

// ---------------------------------------------------------------------------
// ReminderScheduler – startet und stoppt den periodischen Reminder-Worker
//
// Verwendet WorkManager PeriodicWork:
//   • läuft alle 60 Minuten
//   • überlebt App-Neustart und Doze-Mode
//   • eindeutiger Name "water_reminder_periodic" verhindert Duplikate
// ---------------------------------------------------------------------------
object ReminderScheduler {

    // Eindeutiger Name für den periodischen WorkRequest
    // → WorkManager erkennt damit ob bereits ein Job läuft
    private const val WORK_NAME = "water_reminder_periodic"

    // -----------------------------------------------------------------------
    // start – plant den periodischen Reminder-Worker
    //
    // ExistingPeriodicWorkPolicy.UPDATE:
    //   Falls bereits ein Job mit diesem Namen existiert, wird er
    //   AKTUALISIERT (neuer Intervall, neue inputData) statt ignoriert.
    //   → Sinnvoll wenn der Nutzer das Intervall in den Settings ändert.
    //
    // PeriodicWorkRequestBuilder benötigt ein Minimum-Intervall von 15 Min.
    //
    // inputData source="scheduled" → Worker weiß dass es kein Snooze ist
    //   → allowSnooze = true → Snooze-Button wird in der Notification gezeigt
    // -----------------------------------------------------------------------
    fun start(context: Context, intervalMinutes: Long = 60) {
        // WorkManager Minimum: 15 Min. → sicherstellen
        val safeInterval = intervalMinutes.coerceAtLeast(15)

        val workRequest = PeriodicWorkRequestBuilder<WaterReminderWorker>(
            safeInterval, TimeUnit.MINUTES
        )
            .setInputData(
                workDataOf(WaterReminderWorker.KEY_SOURCE to "scheduled")
            )
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            workRequest
        )
    }

    // -----------------------------------------------------------------------
    // stop – bricht den periodischen Reminder-Worker ab
    //
    // cancelUniqueWork() findet den Job anhand seines Namens und bricht ihn ab.
    // Bereits laufende Worker-Instanzen werden nicht unterbrochen –
    // nur zukünftige Ausführungen werden verhindert.
    // -----------------------------------------------------------------------
    fun stop(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }
}

