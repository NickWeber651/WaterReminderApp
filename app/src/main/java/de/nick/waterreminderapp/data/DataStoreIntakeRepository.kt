package de.nick.waterreminderapp.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import de.nick.waterreminderapp.util.SystemTimeProvider
import de.nick.waterreminderapp.util.TimeProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// DataStore-Singleton für den produktiven Einsatz (Context-Delegate)
private val Context.entriesStore: DataStore<Preferences>
    by preferencesDataStore(name = "water_entries")

/**
 * DataStore-Implementierung von [IntakeRepository].
 *
 * Persistenz-Format:
 * Die Einträge werden als einfaches CSV-artiges String-Format gespeichert:
 *   "<id1>:<ml1>,<id2>:<ml2>,..."
 *
 * Warum kein JSON-Parser?
 * Das Format ist trivial genug, um ohne externe Abhängigkeit (Gson/Moshi)
 * auszukommen und hält die Einstiegshürde niedrig.
 *
 * Tageswechsel:
 * [SAVED_DAY] + [SAVED_YEAR] werden zusammen mit den Einträgen gespeichert.
 * Beim Lesen: Stimmt das gespeicherte Datum nicht mit heute überein,
 * wird eine leere Liste geliefert (lazy reset – kein Background-Job nötig).
 *
 * Testbarkeit:
 * Der [dataStore]-Parameter kann im Test durch eine frische In-Memory-Instanz
 * (PreferenceDataStoreFactory.createWithPath / createInMemory) ersetzt werden,
 * damit jeder Test vollständig isoliert läuft.
 */
class DataStoreIntakeRepository(
    private val dataStore: DataStore<Preferences>,
    private val timeProvider: TimeProvider = SystemTimeProvider
) : IntakeRepository {

    /**
     * Produktions-Factory und DataStore-Keys.
     * (Kotlin erlaubt nur ein companion object pro Klasse.)
     */
    companion object {
        private val ENTRIES_CSV = stringPreferencesKey("entries_csv")
        private val SAVED_DAY   = intPreferencesKey("entries_day")
        private val SAVED_YEAR  = intPreferencesKey("entries_year")

        fun create(
            context: Context,
            timeProvider: TimeProvider = SystemTimeProvider
        ): DataStoreIntakeRepository =
            DataStoreIntakeRepository(context.entriesStore, timeProvider)
    }


    // ── Serialisierung ──────────────────────────────────────────────────────

    /** Wandelt eine Liste von Einträgen in einen persistierbaren String um. */
    private fun List<WaterEntry>.toCsv(): String =
        joinToString(",") { "${it.id}:${it.amountMl}" }

    /** Parst den gespeicherten String zurück in eine Liste. Leerer String → leere Liste. */
    private fun String.toEntries(): List<WaterEntry> {
        if (isBlank()) return emptyList()
        return split(",").mapNotNull { token ->
            val parts = token.split(":")
            if (parts.size == 2) {
                val id = parts[0]
                val ml = parts[1].toIntOrNull()
                if (id.isNotBlank() && ml != null && ml > 0) WaterEntry(id, ml) else null
            } else null
        }
    }

    // ── Hilfsfunktionen ─────────────────────────────────────────────────────

    private fun todayDay()  = timeProvider.currentDayOfYear()
    private fun todayYear() = timeProvider.currentYear()
    private fun isSameDay(savedDay: Int, savedYear: Int) =
        savedDay == todayDay() && savedYear == todayYear()

    // ── Flow ────────────────────────────────────────────────────────────────

    override val todayEntriesFlow: Flow<List<WaterEntry>> =
        dataStore.data.map { prefs ->
            val savedDay  = prefs[SAVED_DAY]   ?: 0
            val savedYear = prefs[SAVED_YEAR]  ?: 0
            if (!isSameDay(savedDay, savedYear)) {
                emptyList()
            } else {
                (prefs[ENTRIES_CSV] ?: "").toEntries()
            }
        }

    override val totalMlTodayFlow: Flow<Int> =
        todayEntriesFlow.map { entries -> entries.sumOf { it.amountMl } }

    // ── Mutationen ──────────────────────────────────────────────────────────

    override suspend fun addEntry(amountMl: Int) {
        dataStore.edit { prefs ->
            val savedDay  = prefs[SAVED_DAY]   ?: 0
            val savedYear = prefs[SAVED_YEAR]  ?: 0

            // Bei Tageswechsel: alten Inhalt verwerfen
            val current = if (isSameDay(savedDay, savedYear)) {
                (prefs[ENTRIES_CSV] ?: "").toEntries()
            } else {
                emptyList()
            }

            val updated = current + WaterEntry.create(amountMl)
            prefs[ENTRIES_CSV] = updated.toCsv()
            prefs[SAVED_DAY]   = todayDay()
            prefs[SAVED_YEAR]  = todayYear()
        }
    }

    override suspend fun removeEntry(id: String) {
        dataStore.edit { prefs ->
            val savedDay  = prefs[SAVED_DAY]   ?: 0
            val savedYear = prefs[SAVED_YEAR]  ?: 0
            if (!isSameDay(savedDay, savedYear)) return@edit   // nichts zu löschen

            val current = (prefs[ENTRIES_CSV] ?: "").toEntries()
            prefs[ENTRIES_CSV] = current.filter { it.id != id }.toCsv()
        }
    }
}
