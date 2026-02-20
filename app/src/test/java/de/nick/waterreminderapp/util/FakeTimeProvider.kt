package de.nick.waterreminderapp.util

import java.util.Calendar

class FakeTimeProvider(
    private var hour: Int,
    private var dayOfWeek: Int,
    private var dayOfYear: Int,
    private var year: Int
) : TimeProvider {
    override fun currentHour()      = hour
    override fun currentDayOfWeek() = dayOfWeek
    override fun currentDayOfYear() = dayOfYear
    override fun currentYear()      = year

    fun setHour(h: Int)      { hour = h }
    fun setDayOfWeek(d: Int) { dayOfWeek = d }
    fun advanceToDayOfYear(d: Int, y: Int = year) { dayOfYear = d; year = y }
}

