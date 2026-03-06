package de.nick.waterreminderapp.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/**
 * In-Memory-Implementierung von [IntakeRepository] für Unit-Tests.
 *
 * Kein DataStore, kein Android-Context – reine Kotlin-Klasse.
 * Das macht ViewModel-Tests einfach und schnell (kein Robolectric nötig).
 */
class FakeIntakeRepository : IntakeRepository {

    private val _entries = MutableStateFlow<List<WaterEntry>>(emptyList())

    override val todayEntriesFlow: Flow<List<WaterEntry>> = _entries

    override val totalMlTodayFlow: Flow<Int> =
        _entries.map { list -> list.sumOf { it.amountMl } }

    override suspend fun addEntry(amountMl: Int) {
        _entries.update { it + WaterEntry.create(amountMl) }
    }

    override suspend fun removeEntry(id: String) {
        _entries.update { list -> list.filter { it.id != id } }
    }

    /** Hilfsfunktion für Tests: aktuellen State direkt setzen. */
    fun setEntries(entries: List<WaterEntry>) {
        _entries.value = entries
    }
}

