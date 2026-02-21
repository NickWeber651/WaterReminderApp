package de.nick.waterreminderapp.ui.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsValidatorTest {

    // ── Hilfsfunktion mit Defaults ──────────────────────────────────────────
    private fun validate(
        goalMl:           Int = 2000,
        intervalMinutes:  Int = 60,
        weekdayStartHour: Int = 8,
        weekendStartHour: Int = 9,
        endHour:          Int = 23
    ) = SettingsValidator.validate(goalMl, intervalMinutes, weekdayStartHour, weekendStartHour, endHour)

    // ── goalMl ──────────────────────────────────────────────────────────────

    @Test
    fun goalMlGueltigKeinFehler() {
        // 0 ist ausdrücklich erlaubt (Nutzer möchte Ziel deaktivieren)
        assertNull(validate(goalMl = 0).goalMlError)
        assertNull(validate(goalMl = 250).goalMlError)
        assertNull(validate(goalMl = 2000).goalMlError)
        assertNull(validate(goalMl = 5000).goalMlError)
    }

    @Test
    fun goalMlNegativLiefertFehler() {
        assertNotNull(validate(goalMl = -1).goalMlError)
        assertNotNull(validate(goalMl = -100).goalMlError)
    }

    @Test
    fun goalMlGrenzwertNullIstGueltig() {
        assertNull(validate(goalMl = 0).goalMlError)
    }

    // ── intervalMinutes ─────────────────────────────────────────────────────

    @Test
    fun intervalMinutesGueltigKeinFehler() {
        assertNull(validate(intervalMinutes = 1).intervalMinutesError)
        assertNull(validate(intervalMinutes = 15).intervalMinutesError)
        assertNull(validate(intervalMinutes = 60).intervalMinutesError)
        assertNull(validate(intervalMinutes = 120).intervalMinutesError)
    }

    @Test
    fun intervalMinutesZuKleinLiefertFehler() {
        assertNotNull(validate(intervalMinutes = 0).intervalMinutesError)
        assertNotNull(validate(intervalMinutes = -1).intervalMinutesError)
    }

    @Test
    fun intervalMinutesGrenzwertEinsIstGueltig() {
        assertNull(validate(intervalMinutes = 1).intervalMinutesError)
    }

    // ── Stunden allgemein ───────────────────────────────────────────────────

    @Test
    fun stundenAusserhalbBereichLiefernFehler() {
        assertNotNull(validate(weekdayStartHour = -1).weekdayStartHourError)
        assertNotNull(validate(weekdayStartHour = 24).weekdayStartHourError)
        assertNotNull(validate(weekendStartHour = 24).weekendStartHourError)
        assertNotNull(validate(endHour = 24).endHourError)
    }

    @Test
    fun stundenGrenzwerteGueltig() {
        assertNull(validate(weekdayStartHour = 0, endHour = 1).weekdayStartHourError)
        assertNull(validate(endHour = 23).endHourError)
    }

    // ── endHour > startHour ─────────────────────────────────────────────────

    @Test
    fun startHourGroesserGleichEndHourLiefertFehler() {
        assertNotNull(validate(weekdayStartHour = 23, endHour = 23).weekdayStartHourError)
        assertNotNull(validate(weekdayStartHour = 23, endHour = 22).weekdayStartHourError)
        assertNotNull(validate(weekendStartHour = 10, endHour = 10).weekendStartHourError)
    }

    @Test
    fun startHourKleinerEndHourIstGueltig() {
        assertNull(validate(weekdayStartHour = 8, endHour = 23).weekdayStartHourError)
        assertNull(validate(weekendStartHour = 9, endHour = 23).weekendStartHourError)
        assertNull(validate(weekdayStartHour = 0, endHour = 1).weekdayStartHourError)
    }

    // ── isValid gesamt ──────────────────────────────────────────────────────

    @Test
    fun alleDefaultsGueltig() {
        assertTrue(validate().isValid)
    }

    @Test
    fun nullMlIstGueltig() {
        // 0 ml Tagesziel ist fachlich erlaubt
        assertTrue(validate(goalMl = 0).isValid)
    }

    @Test
    fun negativerWertMachtResultatUngueltig() {
        assertFalse(validate(goalMl = -1).isValid)
    }

    @Test
    fun intervalNullMachtResultatUngueltig() {
        assertFalse(validate(intervalMinutes = 0).isValid)
    }

    @Test
    fun mehrereUngueltigeFelder() {
        val result = validate(goalMl = -5, intervalMinutes = 0, weekdayStartHour = 23, endHour = 22)
        assertFalse(result.isValid)
        assertNotNull(result.goalMlError)
        assertNotNull(result.intervalMinutesError)
        assertNotNull(result.weekdayStartHourError)
    }
}
