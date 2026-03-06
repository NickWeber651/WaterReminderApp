package de.nick.waterreminderapp.ui.home

import de.nick.waterreminderapp.data.FakeIntakeRepository
import de.nick.waterreminderapp.data.Settings
import de.nick.waterreminderapp.ui.settings.FakeSettingsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit-Tests für [HomeViewModel].
 *
 * Kein Android-Context, kein Robolectric – reines Kotlin.
 * [FakeIntakeRepository] und [FakeSettingsStore] ersetzen die echten Abhängigkeiten.
 * [StandardTestDispatcher] gibt uns volle Kontrolle über Coroutine-Ausführung.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repo: FakeIntakeRepository
    private lateinit var settingsStore: FakeSettingsStore
    private lateinit var vm: HomeViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repo = FakeIntakeRepository()
        settingsStore = FakeSettingsStore(Settings(goalMl = 2000))
        vm = HomeViewModel(repo, settingsStore)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ── openAddSheet / closeAddSheet ─────────────────────────────────────

    @Test
    fun `openAddSheet setzt showAddSheet auf true und setzt Input zurueck`() = runTest {
        vm.openAddSheet()
        advanceUntilIdle()
        val state = vm.uiState.first()
        assertTrue(state.showAddSheet)
        assertEquals("250", state.inputText)
        assertNull(state.inputError)
    }

    @Test
    fun `closeAddSheet setzt showAddSheet auf false`() = runTest {
        vm.openAddSheet()
        vm.closeAddSheet()
        advanceUntilIdle()
        assertFalse(vm.uiState.first().showAddSheet)
    }

    // ── onInputChange ────────────────────────────────────────────────────

    @Test
    fun `onInputChange aktualisiert inputText und loescht Fehler`() = runTest {
        vm.openAddSheet()
        vm.onInputChange("0")
        vm.confirmAdd()
        advanceUntilIdle()

        vm.onInputChange("300")
        advanceUntilIdle()
        val state = vm.uiState.first()
        assertEquals("300", state.inputText)
        assertNull(state.inputError)
    }

    // ── confirmAdd – Validierung ─────────────────────────────────────────

    @Test
    fun `confirmAdd mit ungueltigem Wert setzt inputError und schliesst Sheet nicht`() = runTest {
        vm.openAddSheet()
        vm.onInputChange("0")
        vm.confirmAdd()
        advanceUntilIdle()

        val state = vm.uiState.first()
        assertEquals(AddEntryValidator.ERROR_MESSAGE, state.inputError)
        assertTrue(state.showAddSheet)
    }

    @Test
    fun `confirmAdd mit leerem String setzt inputError`() = runTest {
        vm.openAddSheet()
        vm.onInputChange("")
        vm.confirmAdd()
        advanceUntilIdle()
        assertNotNull(vm.uiState.first().inputError)
    }

    @Test
    fun `confirmAdd mit negativem Wert setzt inputError`() = runTest {
        vm.openAddSheet()
        vm.onInputChange("-5")
        vm.confirmAdd()
        advanceUntilIdle()
        assertNotNull(vm.uiState.first().inputError)
    }

    // ── confirmAdd – Erfolgsfall ─────────────────────────────────────────

    @Test
    fun `confirmAdd mit gueltigem Wert schliesst Sheet und fuegt Eintrag hinzu`() = runTest {
        vm.openAddSheet()
        vm.onInputChange("300")
        vm.confirmAdd()
        advanceUntilIdle()

        val state = vm.uiState.first()
        assertFalse(state.showAddSheet)
        assertNull(state.inputError)
        assertEquals(1, state.entries.size)
        assertEquals(300, state.entries[0].amountMl)
        assertEquals(300, state.totalMl)
    }

    @Test
    fun `mehrere confirmAdd-Aufrufe akkumulieren totalMl`() = runTest {
        vm.openAddSheet(); vm.onInputChange("250"); vm.confirmAdd()
        advanceUntilIdle()
        vm.openAddSheet(); vm.onInputChange("500"); vm.confirmAdd()
        advanceUntilIdle()

        val state = vm.uiState.first()
        assertEquals(2, state.entries.size)
        assertEquals(750, state.totalMl)
    }

    // ── deleteEntry ──────────────────────────────────────────────────────

    @Test
    fun `deleteEntry entfernt Eintrag und aktualisiert totalMl`() = runTest {
        vm.openAddSheet(); vm.onInputChange("250"); vm.confirmAdd()
        advanceUntilIdle()

        val id = vm.uiState.first().entries[0].id
        vm.deleteEntry(id)
        advanceUntilIdle()

        val state = vm.uiState.first()
        assertTrue(state.entries.isEmpty())
        assertEquals(0, state.totalMl)
    }

    // ── goalMl aus Settings ──────────────────────────────────────────────

    @Test
    fun `uiState enthaelt goalMl aus Settings`() = runTest {
        advanceUntilIdle()
        assertEquals(2000, vm.uiState.first().goalMl)
    }
}

