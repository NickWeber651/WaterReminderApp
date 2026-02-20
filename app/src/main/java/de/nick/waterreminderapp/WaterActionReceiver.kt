package de.nick.waterreminderapp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

// ---------------------------------------------------------------------------
// WaterActionReceiver – empfängt PendingIntent-Aktionen aus Benachrichtigungen
//
// Aktionen:
//   ACTION_DRANK_250  → Nutzer hat 250 ml getrunken
//   ACTION_SNOOZE_15  → Nutzer möchte 15 Minuten Snooze
//
// Extras:
//   EXTRA_REMINDER_ID → Long – ID des auslösenden Reminders
// ---------------------------------------------------------------------------
class WaterActionReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_DRANK_250  = "de.nick.waterreminderapp.ACTION_DRANK_250"
        const val ACTION_SNOOZE_15  = "de.nick.waterreminderapp.ACTION_SNOOZE_15"
        const val EXTRA_REMINDER_ID = "extra_reminder_id"

        // Standard-Tagesziel falls SettingsStore noch nicht gelesen werden kann
        private const val DEFAULT_GOAL_ML = 2000
    }

    // -----------------------------------------------------------------------
    // onReceive – Einstiegspunkt für alle Broadcast-Aktionen
    //
    // BroadcastReceiver.onReceive() läuft auf dem Main-Thread und darf
    // NICHT blockieren. Für Coroutinen (suspend-Funktionen wie addMl)
    // nutzen wir goAsync() + CoroutineScope(Dispatchers.IO).
    // -----------------------------------------------------------------------
    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getLongExtra(EXTRA_REMINDER_ID, -1L)

        when (intent.action) {
            ACTION_DRANK_250 -> handleDrank(context)
            ACTION_SNOOZE_15 -> handleSnooze(context, reminderId)
        }
    }

    // -----------------------------------------------------------------------
    // handleDrank – verarbeitet "250 ml getrunken"
    //
    // Ablauf:
    //   1. Füge 250 ml zum heutigen Gesamtwert hinzu  (IntakeStore.addMl)
    //   2. Prüfe ob Tagesziel erreicht                (IntakeStore.hasReachedGoal)
    //   3. Prüfe ob Glückwunsch heute schon gesendet  (congratsSentTodayFlow)
    //   4. Wenn Ziel erreicht und noch kein Glückwunsch → zeige Notification
    //      und markiere als gesendet                  (markCongratsSent)
    //   5. Schließe die Reminder-Notification         (cancel NOTIFICATION_ID_REMINDER)
    //
    // goAsync() verlängert die Lebenszeit des Receivers damit die Coroutine
    // fertig laufen kann, bevor Android den Prozess ggf. beendet.
    // -----------------------------------------------------------------------
    private fun handleDrank(context: Context) {
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val intakeStore = IntakeStore(context)
                val notificationHelper = NotificationHelper(context)

                // 1. 250 ml hinzufügen
                intakeStore.addMl(250)

                // 2+3. Ziel erreicht UND Glückwunsch noch nicht gesendet?
                val goalReached  = intakeStore.hasReachedGoal(DEFAULT_GOAL_ML)
                val congratsSent = intakeStore.isCongratsSentToday()

                if (goalReached && !congratsSent) {
                    // 4. Glückwunsch-Notification zeigen und merken
                    notificationHelper.showCongratsNotification()
                    intakeStore.markCongratsSent()
                }

                // 5. Reminder-Notification schließen
                NotificationManagerCompat.from(context)
                    .cancel(NotificationHelper.NOTIFICATION_ID_REMINDER)

            } finally {
                // goAsync() MUSS immer mit finish() abgeschlossen werden
                pendingResult.finish()
            }
        }
    }

    // -----------------------------------------------------------------------
    // handleSnooze – verarbeitet "15 Minuten später"
    //
    // Ablauf:
    //   1. Lese reminderId aus dem Extra
    //   2. Erstelle einen eindeutigen WorkRequest mit 15 Min. Delay
    //      Name: "water_snooze_<reminderId>"
    //      Policy: KEEP → falls schon ein Snooze läuft, nicht überschreiben
    //      inputData: source="snooze"
    //   3. Schließe die aktuelle Reminder-Notification
    //
    // WorkManager ist für verzögerte Hintergrundaufgaben ideal:
    //   - überlebt App-Neustart
    //   - garantierte Ausführung auch nach Doze-Mode
    // -----------------------------------------------------------------------
    private fun handleSnooze(context: Context, reminderId: Long) {
        // Eindeutiger Name pro reminderId verhindert doppelte Snooze-Jobs
        val workName = "water_snooze_$reminderId"

        val workRequest = OneTimeWorkRequestBuilder<WaterReminderWorker>()
            .setInitialDelay(15, TimeUnit.MINUTES)
            .setInputData(
                workDataOf(WaterReminderWorker.KEY_SOURCE to "snooze")
            )
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            workName,
            ExistingWorkPolicy.KEEP,   // läuft bereits ein Snooze → nicht neu starten
            workRequest
        )

        // Aktuelle Reminder-Notification schließen
        NotificationManagerCompat.from(context)
            .cancel(NotificationHelper.NOTIFICATION_ID_REMINDER)
    }
}
