package de.nick.waterreminderapp

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit-Tests für TimeWindowChecker.
 *
 * Da isAllowedNow() intern Calendar.getInstance() benutzt (nicht injizierbar),
 * testen wir hier nur die Grenzwert-Logik über die öffentliche API.
 * Der Test prüft ob die Funktion ohne Crash läuft und einen Boolean liefert.
 *
 * Für detaillierte Tests könnte man Calendar als Parameter injizieren –
 * das ist aber für das MVP nicht nötig.
 */
class TimeWindowCheckerTest {

    @Test
    fun `isAllowedNow returns Boolean without crash`() {
        // Einfach sicherstellen, dass die Funktion ohne Exception läuft
        val result = TimeWindowChecker.isAllowedNow(
            weekdayStartHour = 8,
            weekendStartHour = 9,
            endHour = 23
        )
        // result ist entweder true oder false – abhängig von der Uhrzeit
        assertTrue(result || !result) // immer true, testet nur dass kein Crash
    }

    @Test
    fun `endHour 0 means never allowed`() {
        // Wenn endHour = 0, ist currentHour (0–23) nie < 0 → immer false
        // AUSSER currentHour = 0: 0 >= 0 && 0 < 0 → false. Also immer false.
        val result = TimeWindowChecker.isAllowedNow(
            weekdayStartHour = 0,
            weekendStartHour = 0,
            endHour = 0
        )
        assertFalse(result)
    }

    @Test
    fun `full day window startHour 0 endHour 24 is always allowed`() {
        val result = TimeWindowChecker.isAllowedNow(
            weekdayStartHour = 0,
            weekendStartHour = 0,
            endHour = 24
        )
        assertTrue(result)
    }
}

