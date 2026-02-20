package de.nick.waterreminderapp

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.flow.first

// ---------------------------------------------------------------------------
// WaterReminderWorker – führt die eigentliche Reminder-Logik aus
//
// Wird ausgelöst von:
//   • ReminderScheduler  (periodisch, alle 60 Min.) → source = "scheduled"
//   • WaterActionReceiver (nach Snooze, 15 Min. Delay) → source = "snooze"
//
// Ablauf in doWork():
//   1. Settings lesen  (Zeitfenster + Tagesziel)
//   2. Zeitfenster prüfen via TimeWindowChecker
//   3. Tagesziel prüfen via IntakeStore
//   4. Wenn außerhalb Zeitfenster ODER Ziel bereits erreicht → nichts tun
//   5. Sonst Notification zeigen:
//      source == "snooze" → allowSnooze = false  (kein erneuter Snooze-Button)
//      source != "snooze" → allowSnooze = true
// ---------------------------------------------------------------------------
class WaterReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        // inputData-Key – wird von ReminderScheduler und WaterActionReceiver gesetzt
        const val KEY_SOURCE = "source"

        private const val SOURCE_SNOOZE = "snooze"
    }

    override suspend fun doWork(): Result {

        // -------------------------------------------------------------------
        // 1. Settings einmalig lesen
        //    .first() holt genau einen Wert aus dem Flow und gibt ihn zurück.
        //    Kein dauerhaftes Beobachten nötig – Worker läuft einmalig.
        // -------------------------------------------------------------------
        val settings = SettingsStore(applicationContext).settingsFlow.first()

        // -------------------------------------------------------------------
        // 2. Zeitfenster prüfen
        //    TimeWindowChecker.isAllowedNow() ist eine reine Funktion –
        //    kein suspend, kein DataStore → direkt aufrufbar.
        // -------------------------------------------------------------------
        val inTimeWindow = TimeWindowChecker.isAllowedNow(
            weekdayStartHour = settings.weekdayStartHour,
            weekendStartHour = settings.weekendStartHour,
            endHour          = settings.endHour
        )

        if (!inTimeWindow) {
            // Außerhalb des erlaubten Zeitfensters → still beenden
            return Result.success()
        }

        // -------------------------------------------------------------------
        // 3. Tagesziel prüfen
        //    hasReachedGoal() liest intern totalMlTodayFlow.first()
        // -------------------------------------------------------------------
        val intakeStore  = IntakeStore(applicationContext)
        val goalReached  = intakeStore.hasReachedGoal(settings.goalMl)

        if (goalReached) {
            // Tagesziel bereits erreicht → keine weitere Erinnerung nötig
            return Result.success()
        }

        // -------------------------------------------------------------------
        // 4. Notification zeigen
        //    source == "snooze" → Nutzer hat gerade gesnoozed → kein
        //    erneuter Snooze-Button (verhindert endlose Snooze-Kette)
        // -------------------------------------------------------------------
        val source     = inputData.getString(KEY_SOURCE)
        val allowSnooze = source != SOURCE_SNOOZE

        val reminderId = System.currentTimeMillis()   // eindeutige ID pro Aufruf

        NotificationHelper(applicationContext)
            .showReminderNotification(
                reminderId   = reminderId,
                allowSnooze  = allowSnooze
            )

        return Result.success()
    }
}
