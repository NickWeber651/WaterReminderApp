package de.nick.waterreminderapp

import java.util.Calendar

/**
 * Abstrahiert den Zugriff auf die aktuelle Zeit.
 * → Produktionscode nutzt SystemTimeProvider.
 * → Tests nutzen FakeTimeProvider mit fixer Zeit.
 */
interface TimeProvider {
    fun currentHour(): Int
    fun currentDayOfWeek(): Int   // Calendar.DAY_OF_WEEK: Sun=1 Mon=2 … Sat=7
    fun currentDayOfYear(): Int
    fun currentYear(): Int
}

/** Echtzeit-Implementierung – nutzt Calendar.getInstance() */
object SystemTimeProvider : TimeProvider {
    override fun currentHour(): Int =
        Calendar.getInstance().get(Calendar.HOUR_OF_DAY)

    override fun currentDayOfWeek(): Int =
        Calendar.getInstance().get(Calendar.DAY_OF_WEEK)

    override fun currentDayOfYear(): Int =
        Calendar.getInstance().get(Calendar.DAY_OF_YEAR)

    override fun currentYear(): Int =
        Calendar.getInstance().get(Calendar.YEAR)
}

