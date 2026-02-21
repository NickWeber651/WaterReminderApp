package de.nick.waterreminderapp.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import de.nick.waterreminderapp.data.ISettingsStore
import de.nick.waterreminderapp.scheduler.IReminderScheduler
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
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
    private val settingsStore: ISettingsStore,
    private val scheduler: IReminderScheduler? = null   // null = kein Restart (z.B. im Test ohne Scheduler)
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    // Channel für Einmal-Events: Snackbar wird nur einmal angezeigt, auch bei Recompose
    private val _events = Channel<SettingsEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        // Einmalig aktuelle Settings laden – NICHT collect(), da ein dauerhafter
        // Collector nach jedem save() die User-Eingaben überschreiben würde.
        viewModelScope.launch {
            val settings = settingsStore.settingsFlow.first()
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

        // Schritt 3: Atomar in DataStore schreiben (eine Transaktion statt 5 separate)
        viewModelScope.launch {
            settingsStore.updateAll(goalMl, intervalMinutes, weekdayStartHour, weekendStartHour, endHour)

            // Wenn Reminder aktiv sind: mit neuem Intervall neu starten
            val remindersActive = settingsStore.remindersEnabledFlow
                .first()
            if (remindersActive) {
                scheduler?.start(intervalMinutes.toLong())
            }

            _events.send(SettingsEvent.SavedSuccess)
        }
    }

    // ── Factory ──────────────────────────────────────────────────────────────

    class Factory(
        private val store: ISettingsStore,
        private val scheduler: IReminderScheduler? = null
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            SettingsViewModel(store, scheduler) as T
    }
}

