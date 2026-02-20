package de.nick.waterreminderapp

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

/**
 * Unit-Tests für TimeWindowChecker.
 * Kein Android-Context nötig – reine JVM-Tests.
 */
class TimeWindowCheckerTest {

    // Defaults laut MVP-Spec
    private val weekdayStart = 8
    private val weekendStart = 9
    private val end          = 23

    // ── Wochentage (Mo–Fr) ──────────────────────────────────────────────

    @Test fun `weekday at 08 00 is allowed (inclusive start)`() {
        val fake = FakeTimeProvider(hour = 8, dayOfWeek = Calendar.MONDAY,    dayOfYear = 1, year = 2026)
        assertTrue(TimeWindowChecker.isAllowedNow(weekdayStart, weekendStart, end, fake))
    }

    @Test fun `weekday at 22 59 is allowed (before end)`() {
        val fake = FakeTimeProvider(hour = 22, dayOfWeek = Calendar.WEDNESDAY, dayOfYear = 1, year = 2026)
        assertTrue(TimeWindowChecker.isAllowedNow(weekdayStart, weekendStart, end, fake))
    }

    @Test fun `weekday at 23 00 is NOT allowed (exclusive end)`() {
        val fake = FakeTimeProvider(hour = 23, dayOfWeek = Calendar.FRIDAY,   dayOfYear = 1, year = 2026)
        assertFalse(TimeWindowChecker.isAllowedNow(weekdayStart, weekendStart, end, fake))
    }

    @Test fun `weekday at 07 59 is NOT allowed (before start)`() {
        val fake = FakeTimeProvider(hour = 7, dayOfWeek = Calendar.TUESDAY,   dayOfYear = 1, year = 2026)
        assertFalse(TimeWindowChecker.isAllowedNow(weekdayStart, weekendStart, end, fake))
    }

    @Test fun `weekday at 00 00 is NOT allowed`() {
        val fake = FakeTimeProvider(hour = 0, dayOfWeek = Calendar.THURSDAY,  dayOfYear = 1, year = 2026)
        assertFalse(TimeWindowChecker.isAllowedNow(weekdayStart, weekendStart, end, fake))
    }

    // ── Wochenende (Sa/So) ──────────────────────────────────────────────

    @Test fun `saturday at 09 00 is allowed (inclusive start)`() {
        val fake = FakeTimeProvider(hour = 9, dayOfWeek = Calendar.SATURDAY,  dayOfYear = 1, year = 2026)
        assertTrue(TimeWindowChecker.isAllowedNow(weekdayStart, weekendStart, end, fake))
    }

    @Test fun `sunday at 09 00 is allowed`() {
        val fake = FakeTimeProvider(hour = 9, dayOfWeek = Calendar.SUNDAY,    dayOfYear = 1, year = 2026)
        assertTrue(TimeWindowChecker.isAllowedNow(weekdayStart, weekendStart, end, fake))
    }

    @Test fun `saturday at 08 00 is NOT allowed (weekendStart=9)`() {
        val fake = FakeTimeProvider(hour = 8, dayOfWeek = Calendar.SATURDAY,  dayOfYear = 1, year = 2026)
        assertFalse(TimeWindowChecker.isAllowedNow(weekdayStart, weekendStart, end, fake))
    }

    @Test fun `sunday at 23 00 is NOT allowed (exclusive end)`() {
        val fake = FakeTimeProvider(hour = 23, dayOfWeek = Calendar.SUNDAY,   dayOfYear = 1, year = 2026)
        assertFalse(TimeWindowChecker.isAllowedNow(weekdayStart, weekendStart, end, fake))
    }

    @Test fun `saturday at 22 59 is allowed`() {
        val fake = FakeTimeProvider(hour = 22, dayOfWeek = Calendar.SATURDAY, dayOfYear = 1, year = 2026)
        assertTrue(TimeWindowChecker.isAllowedNow(weekdayStart, weekendStart, end, fake))
    }

    // ── Freitag/Samstag Grenze ──────────────────────────────────────────

    @Test fun `friday at 08 is allowed as weekday`() {
        val fake = FakeTimeProvider(hour = 8, dayOfWeek = Calendar.FRIDAY,    dayOfYear = 1, year = 2026)
        assertTrue(TimeWindowChecker.isAllowedNow(weekdayStart, weekendStart, end, fake))
    }
}
