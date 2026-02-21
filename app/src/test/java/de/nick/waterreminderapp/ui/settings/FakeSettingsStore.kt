package de.nick.waterreminderapp.ui.settings

import de.nick.waterreminderapp.data.ISettingsStore
import de.nick.waterreminderapp.data.Settings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * In-Memory Fake – kein Android-Context nötig.
 * Kann im Test mit beliebigen Initialwerten befüllt werden.
 */
class FakeSettingsStore(
    initial: Settings = Settings(),
    remindersEnabled: Boolean = false
) : ISettingsStore {

    private val _flow = MutableStateFlow(initial)
    override val settingsFlow: Flow<Settings> = _flow

    private val _remindersFlow = MutableStateFlow(remindersEnabled)
    override val remindersEnabledFlow: Flow<Boolean> = _remindersFlow

    // Aufzeichnung der letzten gespeicherten Werte für Assertions
    var lastSaved: Settings = initial
        private set

    var remindersEnabledValue: Boolean = remindersEnabled
        private set

    override suspend fun updateGoalMl(value: Int) {
        lastSaved = lastSaved.copy(goalMl = value)
        _flow.value = lastSaved
    }
    override suspend fun updateStepMl(value: Int) {
        lastSaved = lastSaved.copy(stepMl = value)
        _flow.value = lastSaved
    }
    override suspend fun updateIntervalMinutes(value: Int) {
        lastSaved = lastSaved.copy(intervalMinutes = value)
        _flow.value = lastSaved
    }
    override suspend fun updateWeekdayStartHour(value: Int) {
        lastSaved = lastSaved.copy(weekdayStartHour = value)
        _flow.value = lastSaved
    }
    override suspend fun updateWeekendStartHour(value: Int) {
        lastSaved = lastSaved.copy(weekendStartHour = value)
        _flow.value = lastSaved
    }
    override suspend fun updateEndHour(value: Int) {
        lastSaved = lastSaved.copy(endHour = value)
        _flow.value = lastSaved
    }
    override suspend fun setRemindersEnabled(enabled: Boolean) {
        remindersEnabledValue = enabled
        _remindersFlow.value = enabled
    }
}
