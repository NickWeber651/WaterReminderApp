package de.nick.waterreminderapp.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

data class Settings(
    val goalMl: Int = 2000,
    val stepMl: Int = 250,
    val intervalMinutes: Int = 60,
    val weekdayStartHour: Int = 8,
    val weekendStartHour: Int = 9,
    val endHour: Int = 23
)

class SettingsStore(private val context: Context) {

    private companion object Keys {
        val GOAL_ML            = intPreferencesKey("goal_ml")
        val STEP_ML            = intPreferencesKey("step_ml")
        val INTERVAL_MINUTES   = intPreferencesKey("interval_minutes")
        val WEEKDAY_START_HOUR = intPreferencesKey("weekday_start_hour")
        val WEEKEND_START_HOUR = intPreferencesKey("weekend_start_hour")
        val END_HOUR           = intPreferencesKey("end_hour")
    }

    val settingsFlow: Flow<Settings> = context.dataStore.data.map { prefs ->
        Settings(
            goalMl           = prefs[GOAL_ML]            ?: 2000,
            stepMl           = prefs[STEP_ML]            ?: 250,
            intervalMinutes  = prefs[INTERVAL_MINUTES]   ?: 60,
            weekdayStartHour = prefs[WEEKDAY_START_HOUR] ?: 8,
            weekendStartHour = prefs[WEEKEND_START_HOUR] ?: 9,
            endHour          = prefs[END_HOUR]           ?: 23
        )
    }

    suspend fun updateGoalMl(value: Int)           { context.dataStore.edit { it[GOAL_ML]            = value } }
    suspend fun updateStepMl(value: Int)           { context.dataStore.edit { it[STEP_ML]            = value } }
    suspend fun updateIntervalMinutes(value: Int)  { context.dataStore.edit { it[INTERVAL_MINUTES]   = value } }
    suspend fun updateWeekdayStartHour(value: Int) { context.dataStore.edit { it[WEEKDAY_START_HOUR] = value } }
    suspend fun updateWeekendStartHour(value: Int) { context.dataStore.edit { it[WEEKEND_START_HOUR] = value } }
    suspend fun updateEndHour(value: Int)          { context.dataStore.edit { it[END_HOUR]           = value } }
}

