package de.nick.waterreminderapp

import android.app.Application
import androidx.work.Configuration
import de.nick.waterreminderapp.worker.WaterWorkerFactory

/**
 * Custom Application-Klasse – hier konfigurieren wir WorkManager mit
 * unserer eigenen WorkerFactory, damit WaterReminderWorker korrekt
 * instanziiert wird.
 *
 * WICHTIG: Muss im AndroidManifest.xml als android:name=".WaterReminderApplication"
 * eingetragen sein.
 */
class WaterReminderApplication : Application(), Configuration.Provider {

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(WaterWorkerFactory())
            .build()
}

