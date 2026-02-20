package de.nick.waterreminderapp.util

import java.util.Calendar

object TimeWindowChecker {
    fun isAllowedNow(
        weekdayStartHour: Int,
        weekendStartHour: Int,
        endHour: Int,
        timeProvider: TimeProvider = SystemTimeProvider
    ): Boolean {
        val dayOfWeek   = timeProvider.currentDayOfWeek()
        val currentHour = timeProvider.currentHour()
        val isWeekend   = dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY
        val startHour   = if (isWeekend) weekendStartHour else weekdayStartHour
        return currentHour >= startHour && currentHour < endHour
    }
}

