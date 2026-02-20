package de.nick.waterreminderapp

import java.util.Calendar

/**
 * Fake TimeProvider für Tests – Zeit vollständig kontrollierbar.
 *
 * Beispiel:
 *   val fake = FakeTimeProvider(hour = 10, dayOfWeek = Calendar.MONDAY,
 *                               dayOfYear = 50, year = 2026)
 */
class FakeTimeProvider(
    private var hour: Int,
    private var dayOfWeek: Int,    // Calendar.MONDAY = 2, SATURDAY = 7, SUNDAY = 1
    private var dayOfYear: Int,
    private var year: Int
) : TimeProvider {
    override fun currentHour(): Int      = hour
    override fun currentDayOfWeek(): Int = dayOfWeek
    override fun currentDayOfYear(): Int = dayOfYear
    override fun currentYear(): Int      = year

    fun setHour(h: Int)           { hour = h }
    fun setDayOfWeek(d: Int)      { dayOfWeek = d }
    fun advanceToDayOfYear(d: Int, y: Int = year) { dayOfYear = d; year = y }
}

/**
 * Fake NotificationSender für Tests – zeichnet Aufrufe auf statt Notifications zu senden.
 */
class FakeNotificationSender : NotificationSender {
    data class ReminderCall(val reminderId: Long, val allowSnooze: Boolean)

    val reminderCalls  = mutableListOf<ReminderCall>()
    var congratsCalled = 0

    override fun sendReminder(reminderId: Long, allowSnooze: Boolean): Boolean {
        reminderCalls.add(ReminderCall(reminderId, allowSnooze))
        return true
    }

    override fun sendCongrats(): Boolean {
        congratsCalled++
        return true
    }

    fun reset() {
        reminderCalls.clear()
        congratsCalled = 0
    }
}

