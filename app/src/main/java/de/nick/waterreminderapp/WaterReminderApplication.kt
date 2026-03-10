package de.nick.waterreminderapp

import android.app.Application
import android.util.Log
import androidx.work.Configuration
import de.nick.waterreminderapp.data.SettingsStore
import de.nick.waterreminderapp.scheduler.ReminderScheduler
import de.nick.waterreminderapp.worker.WaterWorkerFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

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

    override fun onCreate() {
        super.onCreate()

        // Reminders automatisch wiederherstellen, falls sie aktiv waren.
        // Fängt den Fall ab, dass nach Force-Stop oder Update
        // kein BOOT_COMPLETED kommt und der Scheduler nicht läuft.
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val store = SettingsStore(this@WaterReminderApplication)
                val enabled = store.remindersEnabledFlow.first()
                if (enabled) {
                    val settings = store.settingsFlow.first()
                    val interval = settings.intervalMinutes.toLong()
                    Log.d("WaterReminderApp", "🔄 Auto-Restart: Reminders aktiv → interval=${interval}min")
                    ReminderScheduler.start(this@WaterReminderApplication, interval)
                }
            } catch (e: Exception) {
                Log.w("WaterReminderApp", "⚠ Auto-Restart fehlgeschlagen", e)
            }
        }
    }
}

