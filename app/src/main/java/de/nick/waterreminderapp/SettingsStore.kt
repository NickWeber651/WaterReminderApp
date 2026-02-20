package de.nick.waterreminderapp

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// ---------------------------------------------------------------------------
// 1. DataStore-Instanz als Kotlin-Extension auf Context
//    → erzeugt eine einzige Instanz pro Prozess (Singleton-Garantie)
// ---------------------------------------------------------------------------
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

// ---------------------------------------------------------------------------
// 2. Settings – reine Daten-Klasse (Value Object)
//    Alle Felder haben Standardwerte, damit sie direkt verwendbar sind.
// ---------------------------------------------------------------------------
data class Settings(
    val goalMl: Int = 2000,          // Tagesziel in Milliliter
    val stepMl: Int = 250,           // Menge pro Trinkschritt
    val intervalMinutes: Int = 60,   // Erinnerungs-Intervall
    val weekdayStartHour: Int = 8,   // Wochentag: Erinnerungen ab dieser Stunde
    val weekendStartHour: Int = 9,   // Wochenende: Erinnerungen ab dieser Stunde
    val endHour: Int = 23            // Erinnerungen bis zu dieser Stunde
)

// ---------------------------------------------------------------------------
// 3. SettingsStore – kapselt alle DataStore-Zugriffe
// ---------------------------------------------------------------------------
class SettingsStore(private val context: Context) {

    // -----------------------------------------------------------------------
    // 3a. Keys – typsichere Schlüssel für jeden gespeicherten Wert
    //     intPreferencesKey erzeugt einen Schlüssel vom Typ Int.
    // -----------------------------------------------------------------------
    private companion object Keys {
        val GOAL_ML              = intPreferencesKey("goal_ml")
        val STEP_ML              = intPreferencesKey("step_ml")
        val INTERVAL_MINUTES     = intPreferencesKey("interval_minutes")
        val WEEKDAY_START_HOUR   = intPreferencesKey("weekday_start_hour")
        val WEEKEND_START_HOUR   = intPreferencesKey("weekend_start_hour")
        val END_HOUR             = intPreferencesKey("end_hour")
    }

    // -----------------------------------------------------------------------
    // 3b. settingsFlow – ein reaktiver Datenstrom (Flow<Settings>)
    //     Jedes Mal, wenn sich ein Wert im DataStore ändert, sendet der Flow
    //     automatisch ein aktualisiertes Settings-Objekt.
    //     Der Elvis-Operator ?: liefert den Default-Wert, falls noch kein
    //     Wert gespeichert wurde.
    // -----------------------------------------------------------------------
    val settingsFlow: Flow<Settings> = context.dataStore.data.map { prefs ->
        Settings(
            goalMl            = prefs[GOAL_ML]            ?: 2000,
            stepMl            = prefs[STEP_ML]            ?: 250,
            intervalMinutes   = prefs[INTERVAL_MINUTES]   ?: 60,
            weekdayStartHour  = prefs[WEEKDAY_START_HOUR] ?: 8,
            weekendStartHour  = prefs[WEEKEND_START_HOUR] ?: 9,
            endHour           = prefs[END_HOUR]           ?: 23
        )
    }

    // -----------------------------------------------------------------------
    // 3c. Update-Funktionen
    //     Jede Funktion ist eine suspend fun → muss aus einer Coroutine
    //     oder einem CoroutineScope heraus aufgerufen werden.
    //     context.dataStore.edit { } öffnet eine atomare Transaktion:
    //     Entweder wird der Wert vollständig geschrieben oder gar nicht.
    // -----------------------------------------------------------------------

    suspend fun updateGoalMl(value: Int) {
        context.dataStore.edit { prefs -> prefs[GOAL_ML] = value }
    }

    suspend fun updateStepMl(value: Int) {
        context.dataStore.edit { prefs -> prefs[STEP_ML] = value }
    }

    suspend fun updateIntervalMinutes(value: Int) {
        context.dataStore.edit { prefs -> prefs[INTERVAL_MINUTES] = value }
    }

    suspend fun updateWeekdayStartHour(value: Int) {
        context.dataStore.edit { prefs -> prefs[WEEKDAY_START_HOUR] = value }
    }

    suspend fun updateWeekendStartHour(value: Int) {
        context.dataStore.edit { prefs -> prefs[WEEKEND_START_HOUR] = value }
    }

    suspend fun updateEndHour(value: Int) {
        context.dataStore.edit { prefs -> prefs[END_HOUR] = value }
    }
}

