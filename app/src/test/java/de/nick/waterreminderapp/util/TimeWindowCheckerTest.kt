package de.nick.waterreminderapp.util

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
}

