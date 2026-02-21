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

/** Abstraktionsschicht – ermöglicht Fake-Implementierungen in Unit-Tests. */
interface ISettingsStore {
    val settingsFlow: Flow<Settings>
    suspend fun updateGoalMl(value: Int)
    suspend fun updateStepMl(value: Int)
    suspend fun updateIntervalMinutes(value: Int)
    suspend fun updateWeekdayStartHour(value: Int)
    suspend fun updateWeekendStartHour(value: Int)
    suspend fun updateEndHour(value: Int)
}

class SettingsStore(private val context: Context) : ISettingsStore {

    private companion object Keys {
        val GOAL_ML            = intPreferencesKey("goal_ml")
        val STEP_ML            = intPreferencesKey("step_ml")
        val INTERVAL_MINUTES   = intPreferencesKey("interval_minutes")
        val WEEKDAY_START_HOUR = intPreferencesKey("weekday_start_hour")
        val WEEKEND_START_HOUR = intPreferencesKey("weekend_start_hour")
        val END_HOUR           = intPreferencesKey("end_hour")
    }

    override val settingsFlow: Flow<Settings> = context.dataStore.data.map { prefs ->
        Settings(
            goalMl           = prefs[GOAL_ML]            ?: 2000,
            stepMl           = prefs[STEP_ML]            ?: 250,
            intervalMinutes  = prefs[INTERVAL_MINUTES]   ?: 60,
            weekdayStartHour = prefs[WEEKDAY_START_HOUR] ?: 8,
            weekendStartHour = prefs[WEEKEND_START_HOUR] ?: 9,
            endHour          = prefs[END_HOUR]           ?: 23
        )
    }

    override suspend fun updateGoalMl(value: Int)           { context.dataStore.edit { it[GOAL_ML]            = value } }
    override suspend fun updateStepMl(value: Int)           { context.dataStore.edit { it[STEP_ML]            = value } }
    override suspend fun updateIntervalMinutes(value: Int)  { context.dataStore.edit { it[INTERVAL_MINUTES]   = value } }
    override suspend fun updateWeekdayStartHour(value: Int) { context.dataStore.edit { it[WEEKDAY_START_HOUR] = value } }
    override suspend fun updateWeekendStartHour(value: Int) { context.dataStore.edit { it[WEEKEND_START_HOUR] = value } }
    override suspend fun updateEndHour(value: Int)          { context.dataStore.edit { it[END_HOUR]           = value } }
}

