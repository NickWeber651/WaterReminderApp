package de.nick.waterreminderapp.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import de.nick.waterreminderapp.data.ISettingsStore
import de.nick.waterreminderapp.data.Settings
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// ── UI-State ────────────────────────────────────────────────────────────────

data class SettingsUiState(
    val goalMlInput:           String = "2000",
    val intervalMinutesInput:  String = "60",
    val weekdayStartHourInput: String = "8",
    val weekendStartHourInput: String = "9",
    val endHourInput:          String = "23",
    val validationResult:      SettingsValidator.ValidationResult = SettingsValidator.ValidationResult(),
    val isLoading:             Boolean = true
)

// ── Einmal-Events (Snackbar) ─────────────────────────────────────────────────

sealed interface SettingsEvent {
    data object SavedSuccess : SettingsEvent
}

// ── ViewModel ────────────────────────────────────────────────────────────────

class SettingsViewModel(
    private val settingsStore: ISettingsStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    // Channel für Einmal-Events: Snackbar wird nur einmal angezeigt, auch bei Recompose
    private val _events = Channel<SettingsEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        // Beim Start aktuelle Settings aus DataStore laden und in UI-State übersetzen
        viewModelScope.launch {
            settingsStore.settingsFlow.collect { settings ->
                _uiState.update { it.copy(
                    goalMlInput           = settings.goalMl.toString(),
                    intervalMinutesInput  = settings.intervalMinutes.toString(),
                    weekdayStartHourInput = settings.weekdayStartHour.toString(),
                    weekendStartHourInput = settings.weekendStartHour.toString(),
                    endHourInput          = settings.endHour.toString(),
                    isLoading             = false
                )}
            }
        }
    }

    // ── Eingabe-Handler ──────────────────────────────────────────────────────

    fun onGoalMlChange(v: String)           { _uiState.update { it.copy(goalMlInput = v) } }
    fun onIntervalMinutesChange(v: String)  { _uiState.update { it.copy(intervalMinutesInput = v) } }
    fun onWeekdayStartHourChange(v: String) { _uiState.update { it.copy(weekdayStartHourInput = v) } }
    fun onWeekendStartHourChange(v: String) { _uiState.update { it.copy(weekendStartHourInput = v) } }
    fun onEndHourChange(v: String)          { _uiState.update { it.copy(endHourInput = v) } }

    // ── Speichern ────────────────────────────────────────────────────────────

    fun save() {
        val state = _uiState.value

        // Schritt 1: Parse-Fehler
        val goalMl           = state.goalMlInput.trim().toIntOrNull()
        val intervalMinutes  = state.intervalMinutesInput.trim().toIntOrNull()
        val weekdayStartHour = state.weekdayStartHourInput.trim().toIntOrNull()
        val weekendStartHour = state.weekendStartHourInput.trim().toIntOrNull()
        val endHour          = state.endHourInput.trim().toIntOrNull()

        val parseError = SettingsValidator.ValidationResult(
            goalMlError           = if (goalMl == null)           "Bitte eine Zahl eingeben" else null,
            intervalMinutesError  = if (intervalMinutes == null)  "Bitte eine Zahl eingeben" else null,
            weekdayStartHourError = if (weekdayStartHour == null) "Bitte eine Zahl eingeben" else null,
            weekendStartHourError = if (weekendStartHour == null) "Bitte eine Zahl eingeben" else null,
            endHourError          = if (endHour == null)          "Bitte eine Zahl eingeben" else null,
        )
        if (!parseError.isValid) {
            _uiState.update { it.copy(validationResult = parseError) }
            return
        }

        // Schritt 2: Fachliche Validierung
        val result = SettingsValidator.validate(
            goalMl!!, intervalMinutes!!, weekdayStartHour!!, weekendStartHour!!, endHour!!
        )
        _uiState.update { it.copy(validationResult = result) }
        if (!result.isValid) return

        // Schritt 3: In DataStore schreiben
        viewModelScope.launch {
            settingsStore.updateGoalMl(goalMl)
            settingsStore.updateIntervalMinutes(intervalMinutes)
            settingsStore.updateWeekdayStartHour(weekdayStartHour)
            settingsStore.updateWeekendStartHour(weekendStartHour)
            settingsStore.updateEndHour(endHour)
            _events.send(SettingsEvent.SavedSuccess)
        }
    }

    // ── Factory ──────────────────────────────────────────────────────────────

    class Factory(private val store: ISettingsStore) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            SettingsViewModel(store) as T
    }
}

