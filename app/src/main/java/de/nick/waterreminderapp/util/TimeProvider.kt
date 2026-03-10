package de.nick.waterreminderapp.util

import java.util.Calendar

interface TimeProvider {
    fun currentHour(): Int
    fun currentMinute(): Int
    fun currentDayOfWeek(): Int
    fun currentDayOfYear(): Int
    fun currentYear(): Int
}

object SystemTimeProvider : TimeProvider {
    override fun currentHour()      = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    override fun currentMinute()    = Calendar.getInstance().get(Calendar.MINUTE)
    override fun currentDayOfWeek() = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
    override fun currentDayOfYear() = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
    override fun currentYear()      = Calendar.getInstance().get(Calendar.YEAR)
}

