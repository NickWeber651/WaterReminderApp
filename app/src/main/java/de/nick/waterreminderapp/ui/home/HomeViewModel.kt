package de.nick.waterreminderapp.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import de.nick.waterreminderapp.data.IntakeRepository
import de.nick.waterreminderapp.data.ISettingsStore
import de.nick.waterreminderapp.data.WaterEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// ── UI-State ────────────────────────────────────────────────────────────────

/**
 * Gesamter UI-State des HomeScreens – ein einziger StateFlow,
 * den die UI 1:1 darstellt (Unidirectional Data Flow).
 */
data class HomeUiState(
    /** Alle heutigen Einträge (für die Historienliste) */
    val entries:         List<WaterEntry> = emptyList(),
    /** Summe aller heutigen Einträge in ml */
    val totalMl:         Int              = 0,
    /** Tagesziel aus den Settings */
    val goalMl:          Int              = 2000,
    /** Steuert ob das Bottom Sheet zum Hinzufügen geöffnet ist */
    val showAddSheet:    Boolean          = false,
    /** Aktueller Text im Eingabefeld (Default: 250 als String) */
    val inputText:       String           = "250",
    /** Fehlermeldung bei ungültiger Eingabe – null wenn kein Fehler */
    val inputError:      String?          = null
)

// ── ViewModel ────────────────────────────────────────────────────────────────

class HomeViewModel(
    private val repository:    IntakeRepository,
    private val settingsStore: ISettingsStore
) : ViewModel() {

    // Interner State für UI-Controls (Sheet, Input, Fehler)
    private val _uiControl = MutableStateFlow(
        HomeUiState()  // nur die UI-Control-Felder sind hier relevant
    )

    /**
     * Kombiniert den Repository-Flow (Einträge + Summe) mit den Settings (Ziel)
     * und dem lokalen UI-Control-State (Sheet, Input, Fehler).
     *
     * Warum combine statt separater StateFlows?
     * So hat die UI immer einen konsistenten Snapshot ohne Race Conditions.
     */
    val uiState: StateFlow<HomeUiState> = combine(
        combine(repository.todayEntriesFlow, repository.totalMlTodayFlow) { entries, total ->
            entries to total
        },
        combine(settingsStore.settingsFlow, _uiControl) { settings, control ->
            settings to control
        }
    ) { (entries, total), (settings, control) ->
        HomeUiState(
            entries      = entries,
            totalMl      = total,
            goalMl       = settings.goalMl,
            showAddSheet = control.showAddSheet,
            inputText    = control.inputText,
            inputError   = control.inputError
        )
    }.stateIn(
        scope        = viewModelScope,
        started      = SharingStarted.Eagerly,
        initialValue = HomeUiState()
    )

    // ── Bottom Sheet ──────────────────────────────────────────────────────

    /** Plus-Button geklickt → Sheet öffnen und Input auf Default zurücksetzen */
    fun openAddSheet() {
        _uiControl.update { it.copy(showAddSheet = true, inputText = "250", inputError = null) }
    }

    /** Sheet schließen (Abbrechen oder nach erfolgreichem Speichern) */
    fun closeAddSheet() {
        _uiControl.update { it.copy(showAddSheet = false, inputError = null) }
    }

    /** Nutzer tippt im Eingabefeld – Fehler sofort zurücksetzen */
    fun onInputChange(text: String) {
        _uiControl.update { it.copy(inputText = text, inputError = null) }
    }

    /**
     * Nutzer bestätigt die Eingabe.
     * Bei gültigem Wert: Eintrag speichern + Sheet schließen.
     * Bei ungültigem Wert: Fehler setzen, Sheet bleibt offen.
     */
    fun confirmAdd() {
        val input = _uiControl.value.inputText
        val error = AddEntryValidator.validate(input)
        if (error != null) {
            _uiControl.update { it.copy(inputError = error) }
            return
        }
        val amountMl = AddEntryValidator.parse(input)
        viewModelScope.launch {
            repository.addEntry(amountMl)
        }
        closeAddSheet()
    }

    // ── Löschen ───────────────────────────────────────────────────────────

    /**
     * Eintrag aus der heutigen Liste entfernen.
     * Der Flow aktualisiert sich automatisch → totalMl und Progress updaten sich sofort.
     */
    fun deleteEntry(id: String) {
        viewModelScope.launch {
            repository.removeEntry(id)
        }
    }

    // ── Factory ───────────────────────────────────────────────────────────

    class Factory(
        private val repository:    IntakeRepository,
        private val settingsStore: ISettingsStore
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            HomeViewModel(repository, settingsStore) as T
    }
}

