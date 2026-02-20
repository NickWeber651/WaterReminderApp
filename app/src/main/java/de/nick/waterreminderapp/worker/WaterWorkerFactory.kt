package de.nick.waterreminderapp.worker

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import de.nick.waterreminderapp.notification.NotificationHelper
import de.nick.waterreminderapp.notification.NotificationSender
import de.nick.waterreminderapp.notification.RealNotificationSender
import de.nick.waterreminderapp.util.SystemTimeProvider
import de.nick.waterreminderapp.util.TimeProvider

class WaterWorkerFactory(
    private val notificationSender: NotificationSender? = null,
    private val timeProvider: TimeProvider = SystemTimeProvider
) : WorkerFactory() {

    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters
    ): ListenableWorker? {
        return when (workerClassName) {
            WaterReminderWorker::class.java.name -> {
                val sender = notificationSender
                    ?: RealNotificationSender(NotificationHelper(appContext))
                WaterReminderWorker(appContext, workerParameters, sender, timeProvider)
            }
            else -> null
        }
    }
}

