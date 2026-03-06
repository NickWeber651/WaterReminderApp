package de.nick.waterreminderapp.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import de.nick.waterreminderapp.util.FakeTimeProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.util.Calendar

/**
 * Unit-Tests für [DataStoreIntakeRepository].
 *
 * Warum kein Robolectric?
 * Durch die DataStore-Injektion in den Konstruktor können wir einen
 * echten (dateibasierten) DataStore via [PreferenceDataStoreFactory] und
 * [TemporaryFolder] nutzen – kein Android-Context, kein Robolectric nötig.
 * Jeder Test bekommt sein eigenes temporäres Verzeichnis → vollständige Isolation.
 */
class DataStoreIntakeRepositoryTest {

    @get:Rule
    val tmpFolder: TemporaryFolder = TemporaryFolder()

    private val YEAR = 2026
    private lateinit var fakeTime: FakeTimeProvider
    private lateinit var dataStore: androidx.datastore.core.DataStore<androidx.datastore.preferences.core.Preferences>
    private lateinit var repo: DataStoreIntakeRepository

    private var dayCounter = 100

    @Before
    fun setup() {
        dayCounter++
        fakeTime = FakeTimeProvider(8, Calendar.MONDAY, dayCounter, YEAR)
        // Jeder Test bekommt einen eigenen DataStore in einem frischen Temp-Verzeichnis
        dataStore = PreferenceDataStoreFactory.create {
            tmpFolder.newFile("test_entries_$dayCounter.preferences_pb")
        }
        repo = DataStoreIntakeRepository(dataStore, fakeTime)
    }

    // ── addEntry ────────────────────────────────────────────────────────────

    @Test
    fun `addEntry speichert Eintrag und Flow liefert ihn`() = runBlocking {
        repo.addEntry(250)
        val entries = repo.todayEntriesFlow.first()
        assertEquals(1, entries.size)
        assertEquals(250, entries[0].amountMl)
    }

    @Test
    fun `mehrere addEntry-Aufrufe akkumulieren Eintraege`() = runBlocking {
        repo.addEntry(250)
        repo.addEntry(500)
        repo.addEntry(150)
        val entries = repo.todayEntriesFlow.first()
        assertEquals(3, entries.size)
        assertEquals(900, entries.sumOf { it.amountMl })
    }

    @Test
    fun `totalMlTodayFlow liefert Summe der Eintraege`() = runBlocking {
        repo.addEntry(300)
        repo.addEntry(200)
        assertEquals(500, repo.totalMlTodayFlow.first())
    }

    // ── removeEntry ─────────────────────────────────────────────────────────

    @Test
    fun `removeEntry entfernt Eintrag korrekt`() = runBlocking {
        repo.addEntry(250)
        repo.addEntry(500)
        val before = repo.todayEntriesFlow.first()
        assertEquals(2, before.size)

        val idToRemove = before[0].id
        repo.removeEntry(idToRemove)

        val after = repo.todayEntriesFlow.first()
        assertEquals(1, after.size)
        assertTrue(after.none { it.id == idToRemove })
    }

    @Test
    fun `removeEntry mit unbekannter ID ist no-op`() = runBlocking {
        repo.addEntry(250)
        repo.removeEntry("nicht-vorhanden-id")
        assertEquals(1, repo.todayEntriesFlow.first().size)
    }

    @Test
    fun `removeEntry aktualisiert totalMlTodayFlow`() = runBlocking {
        repo.addEntry(300)
        repo.addEntry(200)
        val id = repo.todayEntriesFlow.first()[0].id
        repo.removeEntry(id)
        val total = repo.totalMlTodayFlow.first()
        assertTrue("Summe nach Loeschen muss < 500 sein", total < 500)
        assertTrue("Summe nach Loeschen muss > 0 sein", total > 0)
    }

    // ── Tageswechsel ────────────────────────────────────────────────────────

    @Test
    fun `nach Tageswechsel liefert Flow leere Liste`() = runBlocking {
        val yesterday = dayCounter - 1
        fakeTime.advanceToDayOfYear(yesterday, YEAR)
        repo.addEntry(500)
        assertEquals(1, repo.todayEntriesFlow.first().size)

        fakeTime.advanceToDayOfYear(dayCounter, YEAR)
        assertEquals(0, repo.todayEntriesFlow.first().size)
        assertEquals(0, repo.totalMlTodayFlow.first())
    }

    @Test
    fun `nach Tageswechsel kann addEntry neuen Tag starten`() = runBlocking {
        val yesterday = dayCounter - 1
        fakeTime.advanceToDayOfYear(yesterday, YEAR)
        repo.addEntry(1000)

        fakeTime.advanceToDayOfYear(dayCounter, YEAR)
        repo.addEntry(250)

        val entries = repo.todayEntriesFlow.first()
        assertEquals(1, entries.size)
        assertEquals(250, entries[0].amountMl)
    }

    // ── CSV-Robustheit ──────────────────────────────────────────────────────

    /**
     * Hilfsfunktion: Schreibt einen rohen CSV-String direkt in den DataStore,
     * um korrupte Zustände zu simulieren, die durch Bugs oder externe Manipulation
     * entstehen könnten.
     */
    private suspend fun writeRawCsv(csv: String) {
        val csvKey = stringPreferencesKey("entries_csv")
        val dayKey = intPreferencesKey("entries_day")
        val yearKey = intPreferencesKey("entries_year")
        dataStore.edit { prefs ->
            prefs[csvKey]  = csv
            prefs[dayKey]  = fakeTime.currentDayOfYear()
            prefs[yearKey] = fakeTime.currentYear()
        }
    }

    @Test
    fun `korrupter CSV-String wird still ignoriert`() = runBlocking {
        writeRawCsv("abc,,,::")
        val entries = repo.todayEntriesFlow.first()
        assertTrue("Korrupte Tokens sollen gefiltert werden", entries.isEmpty())
        assertEquals(0, repo.totalMlTodayFlow.first())
    }

    @Test
    fun `teilweise korrupter CSV-String liefert nur gueltige Eintraege`() = runBlocking {
        writeRawCsv("id1:250,KAPUTT,id2:500,:,id3:-1")
        val entries = repo.todayEntriesFlow.first()
        assertEquals(2, entries.size) // nur id1:250 und id2:500
        assertEquals(750, repo.totalMlTodayFlow.first())
    }

    @Test
    fun `leerer CSV-String liefert leere Liste`() = runBlocking {
        writeRawCsv("")
        assertEquals(0, repo.todayEntriesFlow.first().size)
    }

    @Test
    fun `CSV-String mit nur Leerzeichen liefert leere Liste`() = runBlocking {
        writeRawCsv("   ")
        assertEquals(0, repo.todayEntriesFlow.first().size)
    }

    @Test
    fun `Eintrag mit ml=0 wird beim Parsen gefiltert`() = runBlocking {
        writeRawCsv("id1:0")
        assertTrue(repo.todayEntriesFlow.first().isEmpty())
    }

    // ── Congrats ────────────────────────────────────────────────────────────

    @Test
    fun `isCongratsSentToday liefert initial false`() = runBlocking {
        assertEquals(false, repo.isCongratsSentToday())
    }

    @Test
    fun `markCongratsSent setzt Flag auf true`() = runBlocking {
        repo.markCongratsSent()
        assertEquals(true, repo.isCongratsSentToday())
    }

    @Test
    fun `nach Tageswechsel wird congratsSent zurueckgesetzt`() = runBlocking {
        repo.markCongratsSent()
        assertTrue(repo.isCongratsSentToday())

        fakeTime.advanceToDayOfYear(dayCounter + 50, YEAR)
        assertEquals(false, repo.isCongratsSentToday())
    }
}
