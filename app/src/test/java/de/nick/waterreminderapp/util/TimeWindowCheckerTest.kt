package de.nick.waterreminderapp.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class TimeWindowCheckerTest {

    private val weekdayStart = 8
    private val weekendStart = 9
    private val end          = 23

    @Test fun `weekday at 08 00 is allowed`() {
        assertTrue(TimeWindowChecker.isAllowedNow(weekdayStart, weekendStart, end,
            FakeTimeProvider(8, Calendar.MONDAY, 1, 2026)))
    }

    @Test fun `weekday at 22 59 is allowed`() {
        assertTrue(TimeWindowChecker.isAllowedNow(weekdayStart, weekendStart, end,
            FakeTimeProvider(22, Calendar.WEDNESDAY, 1, 2026)))
    }

    @Test fun `weekday at 23 00 is NOT allowed`() {
        assertFalse(TimeWindowChecker.isAllowedNow(weekdayStart, weekendStart, end,
            FakeTimeProvider(23, Calendar.FRIDAY, 1, 2026)))
    }

    @Test fun `weekday at 07 59 is NOT allowed`() {
        assertFalse(TimeWindowChecker.isAllowedNow(weekdayStart, weekendStart, end,
            FakeTimeProvider(7, Calendar.TUESDAY, 1, 2026)))
    }

    @Test fun `saturday at 09 00 is allowed`() {
        assertTrue(TimeWindowChecker.isAllowedNow(weekdayStart, weekendStart, end,
            FakeTimeProvider(9, Calendar.SATURDAY, 1, 2026)))
    }

    @Test fun `sunday at 09 00 is allowed`() {
        assertTrue(TimeWindowChecker.isAllowedNow(weekdayStart, weekendStart, end,
            FakeTimeProvider(9, Calendar.SUNDAY, 1, 2026)))
    }

    @Test fun `saturday at 08 00 is NOT allowed`() {
        assertFalse(TimeWindowChecker.isAllowedNow(weekdayStart, weekendStart, end,
            FakeTimeProvider(8, Calendar.SATURDAY, 1, 2026)))
    }

    @Test fun `sunday at 23 00 is NOT allowed`() {
        assertFalse(TimeWindowChecker.isAllowedNow(weekdayStart, weekendStart, end,
            FakeTimeProvider(23, Calendar.SUNDAY, 1, 2026)))
    }

    @Test fun `saturday at 22 is allowed`() {
        assertTrue(TimeWindowChecker.isAllowedNow(weekdayStart, weekendStart, end,
            FakeTimeProvider(22, Calendar.SATURDAY, 1, 2026)))
    }

    @Test fun `friday at 08 is allowed as weekday`() {
        assertTrue(TimeWindowChecker.isAllowedNow(weekdayStart, weekendStart, end,
            FakeTimeProvider(8, Calendar.FRIDAY, 1, 2026)))
    }

    @Test fun `weekday at midnight is NOT allowed`() {
        assertFalse(TimeWindowChecker.isAllowedNow(weekdayStart, weekendStart, end,
            FakeTimeProvider(0, Calendar.THURSDAY, 1, 2026)))
    }

    // ── minutesUntilNextWindow ────────────────────────────────────────────

    @Test fun `Montag 07-00 bis Fensterstart 08-00 sind 60 min`() {
        val tp = FakeTimeProvider(7, Calendar.MONDAY, 1, 2026, minute = 0)
        assertEquals(60L, TimeWindowChecker.minutesUntilNextWindow(weekdayStart, weekendStart, end, tp))
    }

    @Test fun `Montag 07-30 bis Fensterstart 08-00 sind 30 min`() {
        val tp = FakeTimeProvider(7, Calendar.MONDAY, 1, 2026, minute = 30)
        assertEquals(30L, TimeWindowChecker.minutesUntilNextWindow(weekdayStart, weekendStart, end, tp))
    }

    @Test fun `Montag 07-45 bis Fensterstart 08-00 sind 15 min`() {
        val tp = FakeTimeProvider(7, Calendar.MONDAY, 1, 2026, minute = 45)
        assertEquals(15L, TimeWindowChecker.minutesUntilNextWindow(weekdayStart, weekendStart, end, tp))
    }

    @Test fun `Montag 23-00 bis Dienstag 08-00 sind 540 min`() {
        // 1h bis Mitternacht + 8h ab Mitternacht = 9h = 540 min
        val tp = FakeTimeProvider(23, Calendar.MONDAY, 1, 2026, minute = 0)
        assertEquals(540L, TimeWindowChecker.minutesUntilNextWindow(weekdayStart, weekendStart, end, tp))
    }

    @Test fun `Montag 23-30 bis Dienstag 08-00 sind 510 min`() {
        val tp = FakeTimeProvider(23, Calendar.MONDAY, 1, 2026, minute = 30)
        assertEquals(510L, TimeWindowChecker.minutesUntilNextWindow(weekdayStart, weekendStart, end, tp))
    }

    @Test fun `Freitag 23-00 bis Samstag 09-00 sind 600 min`() {
        // Freitag → Samstag (Wochenende) → weekendStart = 9
        // 1h + 9h = 10h = 600 min
        val tp = FakeTimeProvider(23, Calendar.FRIDAY, 1, 2026, minute = 0)
        assertEquals(600L, TimeWindowChecker.minutesUntilNextWindow(weekdayStart, weekendStart, end, tp))
    }

    @Test fun `Sonntag 23-00 bis Montag 08-00 sind 540 min`() {
        // Sonntag → Montag (Wochentag) → weekdayStart = 8
        val tp = FakeTimeProvider(23, Calendar.SUNDAY, 1, 2026, minute = 0)
        assertEquals(540L, TimeWindowChecker.minutesUntilNextWindow(weekdayStart, weekendStart, end, tp))
    }

    @Test fun `Samstag 08-00 vor Wochenende-Fensterstart 09-00 sind 60 min`() {
        val tp = FakeTimeProvider(8, Calendar.SATURDAY, 1, 2026, minute = 0)
        assertEquals(60L, TimeWindowChecker.minutesUntilNextWindow(weekdayStart, weekendStart, end, tp))
    }

    @Test fun `Samstag 08-45 bis 09-00 sind 15 min`() {
        val tp = FakeTimeProvider(8, Calendar.SATURDAY, 1, 2026, minute = 45)
        assertEquals(15L, TimeWindowChecker.minutesUntilNextWindow(weekdayStart, weekendStart, end, tp))
    }

    @Test fun `Mitternacht Dienstag bis 08-00 sind 480 min`() {
        val tp = FakeTimeProvider(0, Calendar.TUESDAY, 1, 2026, minute = 0)
        assertEquals(480L, TimeWindowChecker.minutesUntilNextWindow(weekdayStart, weekendStart, end, tp))
    }
}

