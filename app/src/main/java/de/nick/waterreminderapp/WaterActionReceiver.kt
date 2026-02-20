package de.nick.waterreminderapp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

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
        const val ACTION_DRANK_250     = "de.nick.waterreminderapp.ACTION_DRANK_250"
        const val ACTION_SNOOZE_15     = "de.nick.waterreminderapp.ACTION_SNOOZE_15"
        const val EXTRA_REMINDER_ID    = "extra_reminder_id"
    }

    override fun onReceive(context: Context, intent: Intent) {
        // Wird in einem späteren Schritt implementiert
    }
}

