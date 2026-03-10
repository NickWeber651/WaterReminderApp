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

    /**
     * Berechnet die Minuten bis zum nächsten Zeitfenster-Start.
     *
     * Wird aufgerufen, wenn der Worker **außerhalb** des Zeitfensters feuert,
     * damit er sich exakt zum Beginn des nächsten Fensters neu einplanen kann.
     *
     * Logik:
     *  - Wenn die aktuelle Stunde < startHour (heute) → minutengenaue Differenz bis startHour heute
     *  - Wenn die aktuelle Stunde >= endHour → Differenz bis zum startHour von morgen
     *    (berücksichtigt Wochentag/Wochenende des nächsten Tages)
     */
    @Suppress("UNUSED_PARAMETER") // endHour wird für API-Konsistenz mit isAllowedNow beibehalten
    fun minutesUntilNextWindow(
        weekdayStartHour: Int,
        weekendStartHour: Int,
        endHour: Int,
        timeProvider: TimeProvider = SystemTimeProvider
    ): Long {
        val currentHour   = timeProvider.currentHour()
        val currentMinute = timeProvider.currentMinute()
        val dayOfWeek     = timeProvider.currentDayOfWeek()
        val isWeekend     = dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY
        val todayStart    = if (isWeekend) weekendStartHour else weekdayStartHour

        if (currentHour < todayStart) {
            // Noch vor dem heutigen Fenster → minutengenau bis todayStart:00
            val diffMinutes = (todayStart - currentHour) * 60L - currentMinute
            return diffMinutes.coerceAtLeast(1)
        }

        // Aktuell >= endHour (oder aus anderem Grund außerhalb) → morgen
        // Nächsten Wochentag berechnen ohne Calendar.getInstance():
        // Calendar.DAY_OF_WEEK: SUNDAY=1, MONDAY=2, ..., SATURDAY=7
        val tomorrowDow = if (dayOfWeek == Calendar.SATURDAY) Calendar.SUNDAY else dayOfWeek + 1
        val isTomorrowWeekend = tomorrowDow == Calendar.SATURDAY || tomorrowDow == Calendar.SUNDAY
        val tomorrowStart = if (isTomorrowWeekend) weekendStartHour else weekdayStartHour

        // Minutengenau: Minuten bis Mitternacht + Stunden ab Mitternacht bis tomorrowStart
        val minutesUntilMidnight = (24 - currentHour) * 60L - currentMinute
        return (minutesUntilMidnight + tomorrowStart * 60L)
            .coerceAtLeast(1)
    }
}

