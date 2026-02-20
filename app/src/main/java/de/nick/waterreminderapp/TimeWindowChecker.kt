package de.nick.waterreminderapp

import java.util.Calendar

// ---------------------------------------------------------------------------
// TimeWindowChecker – prüft ob Benachrichtigungen gerade erlaubt sind
//
// Erlaubt wenn:  startHour (inklusiv) <= aktuelle Stunde < endHour (exklusiv)
// Wochentag:     Montag–Freitag  → weekdayStartHour
// Wochenende:    Samstag–Sonntag → weekendStartHour
// ---------------------------------------------------------------------------
object TimeWindowChecker {

    // -----------------------------------------------------------------------
    // isAllowedNow – Hauptfunktion
    //
    // Ablauf:
    //   1. Calendar.getInstance() liefert aktuelle Zeit (minSdk-sicher)
    //   2. DAY_OF_WEEK: Sonntag=1, Montag=2, …, Samstag=7
    //      → Wochenende wenn DAY_OF_WEEK == SUNDAY oder == SATURDAY
    //   3. HOUR_OF_DAY liefert 0–23 (24-Stunden-Format, kein AM/PM)
    //   4. Vergleich: startHour <= currentHour < endHour
    // -----------------------------------------------------------------------
    fun isAllowedNow(
        weekdayStartHour: Int,
        weekendStartHour: Int,
        endHour: Int,
        timeProvider: TimeProvider = SystemTimeProvider   // default: Echtzeit
    ): Boolean {
        val dayOfWeek   = timeProvider.currentDayOfWeek()
        val currentHour = timeProvider.currentHour()

        val isWeekend = dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY
        val startHour = if (isWeekend) weekendStartHour else weekdayStartHour

        return currentHour >= startHour && currentHour < endHour
    }
}
