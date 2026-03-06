package de.nick.waterreminderapp.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
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
    private lateinit var repo: DataStoreIntakeRepository

    private var dayCounter = 100

    @Before
    fun setup() {
        dayCounter++
        fakeTime = FakeTimeProvider(8, Calendar.MONDAY, dayCounter, YEAR)
        // Jeder Test bekommt einen eigenen DataStore in einem frischen Temp-Verzeichnis
        val dataStore = PreferenceDataStoreFactory.create {
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
}
