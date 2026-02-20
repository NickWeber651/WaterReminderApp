package de.nick.waterreminderapp

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

class NotificationHelper(private val context: Context) {

    private val channelId = "water_reminders"

    fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Water Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Reminders to drink water"
            }

            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }

    /**
     * Returns true if notification was shown, false if permission is missing
     */
    fun showTestNotification(): Boolean {
        ensureChannel()

        //   POST_NOTIFICATIONS zur Laufzeit
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            if (!granted) return false
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle("Zeit für Wasser 💧")
            .setContentText("Kurzer Reminder: trink ein Glas Wasser.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        // Sicherheitsnetz: falls doch was schiefgeht
        return try {
            NotificationManagerCompat.from(context).notify(1001, notification)
            true
        } catch (e: SecurityException) {
            false
        }
    }
}
