package de.nick.waterreminderapp

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

class NotificationHelper(private val context: Context) {

    // -----------------------------------------------------------------------
    // Konstanten
    // -----------------------------------------------------------------------
    companion object {
        const val CHANNEL_ID            = "water_reminders"

        // Notification-IDs – jede Notification braucht eine eindeutige ID
        const val NOTIFICATION_ID_REMINDER  = 2001
        const val NOTIFICATION_ID_CONGRATS  = 2002

        // Request-Codes für PendingIntent – müssen unterschiedlich sein!
        private const val REQUEST_DRANK     = 100
        private const val REQUEST_SNOOZE    = 101
    }

    // -----------------------------------------------------------------------
    // ensureChannel – erstellt den Notification-Kanal (nur Android 8+)
    //
    // Kanäle müssen einmalig registriert werden. Existiert der Kanal bereits,
    // macht createNotificationChannel() nichts – sicherer Mehrfachaufruf.
    // -----------------------------------------------------------------------
    fun ensureChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Water Reminders",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Reminders to drink water"
        }
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(channel)
    }

    // -----------------------------------------------------------------------
    // hasPermission – prüft POST_NOTIFICATIONS auf Android 13+ (API 33)
    //
    // Auf Android ≤ 12 ist keine Laufzeit-Permission nötig → immer true.
    // -----------------------------------------------------------------------
    private fun hasPermission(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        }
        return true
    }

    // -----------------------------------------------------------------------
    // buildActionIntent – Hilfsfunktion für PendingIntent-Aktionen
    //
    // Erstellt einen PendingIntent der WaterActionReceiver auslöst.
    //
    // FLAG_UPDATE_CURRENT → falls schon ein PendingIntent mit diesem
    //                        requestCode existiert, Extras aktualisieren
    // FLAG_IMMUTABLE      → ab Android 12 Pflicht für nicht-mutable PIs
    // -----------------------------------------------------------------------
    private fun buildActionIntent(action: String, reminderId: Long, requestCode: Int): PendingIntent {
        val intent = Intent(context, WaterActionReceiver::class.java).apply {
            this.action = action
            putExtra(WaterActionReceiver.EXTRA_REMINDER_ID, reminderId)
        }
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    // -----------------------------------------------------------------------
    // showReminderNotification – zeigt die Trink-Erinnerung
    //
    // Parameter:
    //   reminderId   → wird als Extra an WaterActionReceiver übergeben
    //   allowSnooze  → ob die SNOOZE_15-Aktion angehängt werden soll
    //
    // Gibt true zurück wenn die Notification gezeigt wurde,
    // false wenn die Permission fehlt.
    // -----------------------------------------------------------------------
    fun showReminderNotification(reminderId: Long, allowSnooze: Boolean): Boolean {
        ensureChannel()
        if (!hasPermission()) return false

        // PendingIntent für "250 ml getrunken"-Aktion
        val drankPi = buildActionIntent(
            action      = WaterActionReceiver.ACTION_DRANK_250,
            reminderId  = reminderId,
            requestCode = REQUEST_DRANK
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle("Zeit für Wasser 💧")
            .setContentText("Trink jetzt ein Glas Wasser!")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            // Aktion 1: Nutzer hat getrunken
            .addAction(
                android.R.drawable.ic_menu_add,
                "250 ml getrunken",
                drankPi
            )

        // Aktion 2: Snooze – nur wenn erlaubt
        if (allowSnooze) {
            val snoozePi = buildActionIntent(
                action      = WaterActionReceiver.ACTION_SNOOZE_15,
                reminderId  = reminderId,
                requestCode = REQUEST_SNOOZE
            )
            builder.addAction(
                android.R.drawable.ic_menu_recent_history,
                "15 Min. später",
                snoozePi
            )
        }

        return try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_REMINDER, builder.build())
            true
        } catch (_: SecurityException) {
            false
        }
    }

    // -----------------------------------------------------------------------
    // showCongratsNotification – zeigt die Glückwunsch-Notification
    //
    // Wird einmalig pro Tag gezeigt wenn das Tagesziel erreicht wurde.
    // Keine Aktions-Buttons nötig – nur eine Info-Notification.
    // -----------------------------------------------------------------------
    fun showCongratsNotification(): Boolean {
        ensureChannel()
        if (!hasPermission()) return false

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.star_big_on)
            .setContentTitle("Tagesziel erreicht! 🎉")
            .setContentText("Super! Du hast heute dein Wasserziel geschafft.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        return try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_CONGRATS, notification)
            true
        } catch (_: SecurityException) {
            false
        }
    }
}
