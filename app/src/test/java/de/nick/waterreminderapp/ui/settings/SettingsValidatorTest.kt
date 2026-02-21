package de.nick.waterreminderapp.ui.settings

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsValidatorTest {

    // ── Hilfsfunktion mit validen 1.0-Defaults ──────────────────────────────
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
        assertNull(validate(goalMl = 250).goalMlError)
        assertNull(validate(goalMl = 2000).goalMlError)
        assertNull(validate(goalMl = 5000).goalMlError)
    }

    @Test
    fun goalMlGrenzwert250IstGueltig() {
        // Exakt das Minimum muss gültig sein
        assertNull(validate(goalMl = 250).goalMlError)
    }

    @Test
    fun goalMlUnterMinimumLiefertFehler() {
        // 249 ist unter dem Minimum
        assertNotNull(validate(goalMl = 249).goalMlError)
        assertNotNull(validate(goalMl = 0).goalMlError)
        assertNotNull(validate(goalMl = -1).goalMlError)
    }

    @Test
    fun goalMlNullIstUngueltig() {
        // 0 würde hasReachedGoal(0) immer true liefern → Worker sendet nie Notifications
        assertNotNull(validate(goalMl = 0).goalMlError)
        assertFalse(validate(goalMl = 0).isValid)
    }

    // ── intervalMinutes ─────────────────────────────────────────────────────

    @Test
    fun intervalMinutesGueltigKeinFehler() {
        // 1 Min. ist Minimum (OneTime-Modus), alles drüber auch gültig
        assertNull(validate(intervalMinutes = 1).intervalMinutesError)
        assertNull(validate(intervalMinutes = 14).intervalMinutesError)
        assertNull(validate(intervalMinutes = 15).intervalMinutesError)
        assertNull(validate(intervalMinutes = 60).intervalMinutesError)
        assertNull(validate(intervalMinutes = 120).intervalMinutesError)
    }

    @Test
    fun intervalMinutesGrenzwertEinsIstGueltig() {
        assertNull(validate(intervalMinutes = 1).intervalMinutesError)
    }

    @Test
    fun intervalMinutesNullIstUngueltig() {
        // 0 wäre eine Endlosschleife
        assertNotNull(validate(intervalMinutes = 0).intervalMinutesError)
        assertFalse(validate(intervalMinutes = 0).isValid)
    }

    @Test
    fun intervalMinutesNegativIstUngueltig() {
        assertNotNull(validate(intervalMinutes = -1).intervalMinutesError)
    }

    // ── Stunden: Bereich 0..23 ───────────────────────────────────────────────

    @Test
    fun stundenAusserhalbBereichLiefernFehler() {
        assertNotNull(validate(weekdayStartHour = -1).weekdayStartHourError)
        assertNotNull(validate(weekdayStartHour = 24).weekdayStartHourError)
        assertNotNull(validate(weekendStartHour = -1).weekendStartHourError)
        assertNotNull(validate(weekendStartHour = 24).weekendStartHourError)
        assertNotNull(validate(endHour = -1).endHourError)
        assertNotNull(validate(endHour = 24).endHourError)
    }

    @Test
    fun stundenGrenzwerteGueltig() {
        // Stunde 0 als Start mit Stunde 1 als Ende ist gültig
        assertNull(validate(weekdayStartHour = 0, endHour = 1).weekdayStartHourError)
        assertNull(validate(weekendStartHour = 0, endHour = 1).weekendStartHourError)
        // Stunde 23 als Ende ist gültig
        assertNull(validate(endHour = 23).endHourError)
    }

    // ── endHour > startHour (beide Richtungen) ───────────────────────────────

    @Test
    fun weekdayStartGleichEndHourIstUngueltig() {
        // endHour == weekdayStartHour → kein Zeitfenster
        assertNotNull(validate(weekdayStartHour = 8, endHour = 8).weekdayStartHourError)
    }

    @Test
    fun weekdayStartGroesserAlsEndHourIstUngueltig() {
        assertNotNull(validate(weekdayStartHour = 10, endHour = 9).weekdayStartHourError)
        assertNotNull(validate(weekdayStartHour = 23, endHour = 22).weekdayStartHourError)
    }

    @Test
    fun weekendStartGleichEndHourIstUngueltig() {
        assertNotNull(validate(weekendStartHour = 9, endHour = 9).weekendStartHourError)
    }

    @Test
    fun weekendStartGroesserAlsEndHourIstUngueltig() {
        assertNotNull(validate(weekendStartHour = 12, endHour = 10).weekendStartHourError)
    }

    @Test
    fun startKleinerAlsEndHourIstGueltig() {
        assertNull(validate(weekdayStartHour = 8,  endHour = 23).weekdayStartHourError)
        assertNull(validate(weekendStartHour = 9,  endHour = 23).weekendStartHourError)
        assertNull(validate(weekdayStartHour = 0,  endHour = 1).weekdayStartHourError)
        assertNull(validate(weekendStartHour = 0,  endHour = 1).weekendStartHourError)
    }

    @Test
    fun fehlermeldungEnthaeltEndzeit() {
        // Fehlermeldung soll dem Nutzer die Endzeit nennen
        val error = validate(weekdayStartHour = 22, endHour = 20).weekdayStartHourError
        assertNotNull(error)
        assertTrue("Fehlermeldung muss die Endzeit enthalten", error!!.contains("20"))
    }

    // ── isValid gesamt ───────────────────────────────────────────────────────

    @Test
    fun alleDefaultsGueltig() {
        assertTrue(validate().isValid)
    }

    @Test
    fun minimalGueltigeKombination() {
        // Kleinstmögliche valide Eingabe
        assertTrue(validate(goalMl = 250, intervalMinutes = 1,
            weekdayStartHour = 0, weekendStartHour = 0, endHour = 1).isValid)
    }

    @Test
    fun einFehlerMachtResultatUngueltig() {
        assertFalse(validate(goalMl = 249).isValid)
        assertFalse(validate(intervalMinutes = 0).isValid)
        assertFalse(validate(weekdayStartHour = 23, endHour = 23).isValid)
        assertFalse(validate(endHour = 24).isValid)
    }

    @Test
    fun mehrereUngueltigeFelder() {
        val result = validate(
            goalMl           = 100,
            intervalMinutes  = 0,
            weekdayStartHour = 23,
            weekendStartHour = 23,
            endHour          = 22
        )
        assertFalse(result.isValid)
        assertNotNull(result.goalMlError)
        assertNotNull(result.intervalMinutesError)
        assertNotNull(result.weekdayStartHourError)
        assertNotNull(result.weekendStartHourError)
    }
}
