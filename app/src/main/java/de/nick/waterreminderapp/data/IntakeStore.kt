package de.nick.waterreminderapp.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import de.nick.waterreminderapp.util.TimeProvider
import de.nick.waterreminderapp.util.SystemTimeProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.intakeStore: DataStore<Preferences> by preferencesDataStore(name = "intake")

class IntakeStore(
    private val context: Context,
    private val timeProvider: TimeProvider = SystemTimeProvider
) {
    private companion object Keys {
        val TOTAL_ML_TODAY = intPreferencesKey("total_ml_today")
        val SAVED_DAY      = intPreferencesKey("saved_day")
        val SAVED_YEAR     = intPreferencesKey("saved_year")
        val CONGRATS_SENT  = booleanPreferencesKey("congrats_sent")
        val CONGRATS_DAY   = intPreferencesKey("congrats_day")
        val CONGRATS_YEAR  = intPreferencesKey("congrats_year")
    }

    private fun todayDayOfYear() = timeProvider.currentDayOfYear()
    private fun todayYear()      = timeProvider.currentYear()
    private fun isSameDay(savedDay: Int, savedYear: Int) =
        savedDay == todayDayOfYear() && savedYear == todayYear()

    val totalMlTodayFlow: Flow<Int> = context.intakeStore.data.map { prefs ->
        val savedDay  = prefs[SAVED_DAY]  ?: 0
        val savedYear = prefs[SAVED_YEAR] ?: 0
        if (!isSameDay(savedDay, savedYear)) 0 else prefs[TOTAL_ML_TODAY] ?: 0
    }

    suspend fun addMl(ml: Int) {
        context.intakeStore.edit { prefs ->
            val today     = todayDayOfYear()
            val year      = todayYear()
            val savedDay  = prefs[SAVED_DAY]  ?: 0
            val savedYear = prefs[SAVED_YEAR] ?: 0
            val current   = if (!isSameDay(savedDay, savedYear)) 0 else prefs[TOTAL_ML_TODAY] ?: 0
            prefs[TOTAL_ML_TODAY] = current + ml
            prefs[SAVED_DAY]      = today
            prefs[SAVED_YEAR]     = year
        }
    }

    suspend fun hasReachedGoal(goalMl: Int): Boolean = totalMlTodayFlow.first() >= goalMl

    suspend fun markCongratsSent() {
        context.intakeStore.edit { prefs ->
            prefs[CONGRATS_SENT] = true
            prefs[CONGRATS_DAY]  = todayDayOfYear()
            prefs[CONGRATS_YEAR] = todayYear()
        }
    }

    val congratsSentTodayFlow: Flow<Boolean> = context.intakeStore.data.map { prefs ->
        val savedDay  = prefs[CONGRATS_DAY]  ?: 0
        val savedYear = prefs[CONGRATS_YEAR] ?: 0
        if (!isSameDay(savedDay, savedYear)) false else prefs[CONGRATS_SENT] ?: false
    }

    suspend fun isCongratsSentToday(): Boolean = congratsSentTodayFlow.first()
}

