package de.nick.waterreminderapp

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters

/**
 * Eigene WorkerFactory, die NotificationSender und TimeProvider injiziert.
 *
 * Wird in WorkManager-Integrationstests verwendet:
 *   val config = Configuration.Builder()
 *       .setWorkerFactory(TestWorkerFactory(fakeSender, fakeTime))
 *       .build()
 *   WorkManagerTestInitHelper.initializeTestWorkManager(context, config)
 */
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
            else -> null  // Andere Worker: WorkManager nutzt DefaultWorkerFactory
        }
    }
}
