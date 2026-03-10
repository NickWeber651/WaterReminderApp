package de.nick.waterreminderapp.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import de.nick.waterreminderapp.data.SettingsStore
import de.nick.waterreminderapp.scheduler.ReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Stellt nach einem Geräte-Neustart die Erinnerungen wieder her.
 *
 * WorkManager überlebt zwar Reboots für enqueued Work, aber wenn das Gerät
 * lange aus war oder der OneTimeWork bereits abgelaufen ist, geht der
 * nächste geplante Reminder verloren. Dieser Receiver startet die Kette
 * zuverlässig neu.
 */
class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        Log.d(TAG, "📱 BOOT_COMPLETED empfangen")

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val store = SettingsStore(context)
                val enabled = store.remindersEnabledFlow.first()

                if (enabled) {
                    val settings = store.settingsFlow.first()
                    val interval = settings.intervalMinutes.toLong()
                    Log.d(TAG, "✅ Reminders aktiv → Scheduler starten (interval=${interval}min)")
                    ReminderScheduler.start(context, interval)
                } else {
                    Log.d(TAG, "⏸ Reminders deaktiviert → nichts tun")
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}

