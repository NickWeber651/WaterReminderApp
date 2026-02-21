package de.nick.waterreminderapp.ui.settings

import de.nick.waterreminderapp.data.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
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

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ── Hilfsfunktion ────────────────────────────────────────────────────────

    private fun createVm(initial: Settings = Settings()) =
        SettingsViewModel(FakeSettingsStore(initial))

    // ── Laden ────────────────────────────────────────────────────────────────

    @Test
    fun laedeDefaultsAusStore() = runTest {
        val initial = Settings(goalMl = 1500, intervalMinutes = 30, weekdayStartHour = 7, weekendStartHour = 10, endHour = 22)
        val vm = createVm(initial)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals("1500", state.goalMlInput)
        assertEquals("30",   state.intervalMinutesInput)
        assertEquals("7",    state.weekdayStartHourInput)
        assertEquals("10",   state.weekendStartHourInput)
        assertEquals("22",   state.endHourInput)
    }

    @Test
    fun isLoadingFalseNachInit() = runTest {
        val vm = createVm()
        testDispatcher.scheduler.advanceUntilIdle()
        assertFalse(vm.uiState.value.isLoading)
    }

    // ── Eingabe-Handler ──────────────────────────────────────────────────────

    @Test
    fun onGoalMlChangeAktualisiertState() = runTest {
        val vm = createVm()
        vm.onGoalMlChange("3000")
        assertEquals("3000", vm.uiState.value.goalMlInput)
    }

    @Test
    fun onIntervalMinutesChangeAktualisiertState() = runTest {
        val vm = createVm()
        vm.onIntervalMinutesChange("45")
        assertEquals("45", vm.uiState.value.intervalMinutesInput)
    }

    // ── Speichern – Erfolg ───────────────────────────────────────────────────

    @Test
    fun saveGueltigeWerteSchreibtInStore() = runTest {
        val store = FakeSettingsStore()
        val vm    = SettingsViewModel(store)
        testDispatcher.scheduler.advanceUntilIdle()

        vm.onGoalMlChange("1000")
        vm.onIntervalMinutesChange("30")
        vm.onWeekdayStartHourChange("7")
        vm.onWeekendStartHourChange("8")
        vm.onEndHourChange("21")

        vm.save()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1000, store.lastSaved.goalMl)
        assertEquals(30,   store.lastSaved.intervalMinutes)
        assertEquals(7,    store.lastSaved.weekdayStartHour)
        assertEquals(8,    store.lastSaved.weekendStartHour)
        assertEquals(21,   store.lastSaved.endHour)
    }

    @Test
    fun saveGueltigSendetSavedSuccessEvent() = runTest {
        val vm = createVm()
        testDispatcher.scheduler.advanceUntilIdle()

        vm.save() // Defaults sind gültig
        testDispatcher.scheduler.advanceUntilIdle()

        val event = vm.events.first()
        assertEquals(SettingsEvent.SavedSuccess, event)
    }

    @Test
    fun saveGueltigKeinValidierungsfehler() = runTest {
        val vm = createVm()
        testDispatcher.scheduler.advanceUntilIdle()

        vm.save()
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(vm.uiState.value.validationResult.isValid)
    }

    // ── Speichern – Validierungsfehler ───────────────────────────────────────

    @Test
    fun saveNegativesGoalMlSpeichertNicht() = runTest {
        val store = FakeSettingsStore()
        val vm    = SettingsViewModel(store)
        testDispatcher.scheduler.advanceUntilIdle()

        vm.onGoalMlChange("-10")
        vm.save()
        testDispatcher.scheduler.advanceUntilIdle()

        assertNotNull(vm.uiState.value.validationResult.goalMlError)
        // Store darf nicht überschrieben worden sein
        assertEquals(Settings().goalMl, store.lastSaved.goalMl)
    }

    @Test
    fun saveLeererInputSpeichertNicht() = runTest {
        val store = FakeSettingsStore()
        val vm    = SettingsViewModel(store)
        testDispatcher.scheduler.advanceUntilIdle()

        vm.onGoalMlChange("")
        vm.save()
        testDispatcher.scheduler.advanceUntilIdle()

        assertNotNull(vm.uiState.value.validationResult.goalMlError)
        assertEquals(Settings().goalMl, store.lastSaved.goalMl)
    }

    @Test
    fun saveUngueltigeStundeSpeichertNicht() = runTest {
        val store = FakeSettingsStore()
        val vm    = SettingsViewModel(store)
        testDispatcher.scheduler.advanceUntilIdle()

        vm.onEndHourChange("25")
        vm.save()
        testDispatcher.scheduler.advanceUntilIdle()

        assertNotNull(vm.uiState.value.validationResult.endHourError)
        assertEquals(Settings().endHour, store.lastSaved.endHour)
    }

    // ── goalMl = 0 ist seit 1.0 ungültig ────────────────────────────────────

    @Test
    fun saveNullMlIstUngueltigUndWirdNichtGespeichert() = runTest {
        val store = FakeSettingsStore()
        val vm    = SettingsViewModel(store)
        testDispatcher.scheduler.advanceUntilIdle()

        vm.onGoalMlChange("0")
        vm.save()
        testDispatcher.scheduler.advanceUntilIdle()

        // Fehler erwartet, Store darf nicht überschrieben worden sein
        assertNotNull(vm.uiState.value.validationResult.goalMlError)
        assertEquals(Settings().goalMl, store.lastSaved.goalMl)
    }

    // ── Scheduler Restart ────────────────────────────────────────────────────

    @Test
    fun saveMitAktivenReminderStartetSchedulerNeu() = runTest {
        val store     = FakeSettingsStore(remindersEnabled = true)
        val scheduler = FakeReminderScheduler()
        val vm        = SettingsViewModel(store, scheduler)
        testDispatcher.scheduler.advanceUntilIdle()

        vm.onIntervalMinutesChange("45")
        vm.save()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, scheduler.startCalls.size)
        assertEquals(45L, scheduler.startCalls.first())
    }

    @Test
    fun saveMitInaktivenReminderStartetSchedulerNicht() = runTest {
        val store     = FakeSettingsStore(remindersEnabled = false)
        val scheduler = FakeReminderScheduler()
        val vm        = SettingsViewModel(store, scheduler)
        testDispatcher.scheduler.advanceUntilIdle()

        vm.save()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(0, scheduler.startCalls.size)
    }

    @Test
    fun saveVerwendetGespeichertesIntervallBeimRestart() = runTest {
        val store     = FakeSettingsStore(remindersEnabled = true)
        val scheduler = FakeReminderScheduler()
        val vm        = SettingsViewModel(store, scheduler)
        testDispatcher.scheduler.advanceUntilIdle()

        vm.onIntervalMinutesChange("120")
        vm.save()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(120L, scheduler.startCalls.first())
    }
}

