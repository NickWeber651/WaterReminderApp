package de.nick.waterreminderapp.notification

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
import de.nick.waterreminderapp.receiver.WaterActionReceiver

class NotificationHelper(private val context: Context) {

    companion object {
        const val CHANNEL_ID               = "water_reminders"
        const val NOTIFICATION_ID_REMINDER = 2001
        const val NOTIFICATION_ID_CONGRATS = 2002
        private const val REQUEST_DRANK    = 100
        private const val REQUEST_SNOOZE   = 101
    }

    fun ensureChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID, "Water Reminders", NotificationManager.IMPORTANCE_DEFAULT
        ).apply { description = "Reminders to drink water" }
        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .createNotificationChannel(channel)
    }

    private fun hasPermission(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        }
        return true
    }

    private fun buildActionIntent(action: String, reminderId: Long, requestCode: Int): PendingIntent {
        val intent = Intent(context, WaterActionReceiver::class.java).apply {
            this.action = action
            putExtra(WaterActionReceiver.EXTRA_REMINDER_ID, reminderId)
        }
        return PendingIntent.getBroadcast(
            context, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun showReminderNotification(reminderId: Long, allowSnooze: Boolean): Boolean {
        ensureChannel()
        if (!hasPermission()) return false

        // Alte Reminder-Notification zuerst entfernen, damit die neue
        // garantiert als frische Notification mit Sound/Heads-Up erscheint.
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID_REMINDER)

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle("Zeit für Wasser 💧")
            .setContentText("Trink jetzt ein Glas Wasser!")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setWhen(System.currentTimeMillis())
            .setShowWhen(true)
            .addAction(android.R.drawable.ic_menu_add, "250 ml getrunken",
                buildActionIntent(WaterActionReceiver.ACTION_DRANK_250, reminderId, REQUEST_DRANK))

        if (allowSnooze) {
            builder.addAction(android.R.drawable.ic_menu_recent_history, "15 Min. später",
                buildActionIntent(WaterActionReceiver.ACTION_SNOOZE_15, reminderId, REQUEST_SNOOZE))
        }

        return try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_REMINDER, builder.build())
            true
        } catch (_: SecurityException) { false }
    }

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
        } catch (_: SecurityException) { false }
    }
}

