package de.nick.waterreminderapp.ui.settings

/**
 * Zustandslose Validierungs-Logik für den Settings-Screen.
 *
 * 1.0-Regeln (bewusst festgelegt und konsistent mit Worker-Logik):
 *
 *   goalMl          >= 250  → 0 würde hasReachedGoal(0) immer true machen → nie Notification
 *   intervalMinutes >= 1    → 0 wäre Endlosschleife; < 15 → OneTime-Modus (erlaubt)
 *   hours           0..23   → gültiger Uhrzeit-Bereich
 *   endHour > weekdayStartHour  → sonst kein sinnvolles Zeitfenster
 *   endHour > weekendStartHour  → dito
 *
 * Bewusst ohne Android-Abhängigkeiten – direkt in JVM Unit-Tests testbar.
 */
object SettingsValidator {

    const val GOAL_ML_MIN          = 250  // < 250 → Worker würde nie benachrichtigen
    const val INTERVAL_MINUTES_MIN = 1    // < 15 → OneTimeWorkRequest-Modus
    const val HOUR_MIN             = 0
    const val HOUR_MAX             = 23

    data class ValidationResult(
        val goalMlError:           String? = null,
        val intervalMinutesError:  String? = null,
        val weekdayStartHourError: String? = null,
        val weekendStartHourError: String? = null,
        val endHourError:          String? = null
    ) {
        val isValid: Boolean get() =
            goalMlError           == null &&
            intervalMinutesError  == null &&
            weekdayStartHourError == null &&
            weekendStartHourError == null &&
            endHourError          == null
    }

    fun validate(
        goalMl:           Int,
        intervalMinutes:  Int,
        weekdayStartHour: Int,
        weekendStartHour: Int,
        endHour:          Int
    ): ValidationResult = ValidationResult(
        goalMlError = when {
            goalMl < GOAL_ML_MIN -> "Mindestens $GOAL_ML_MIN ml"
            else                 -> null
        },
        intervalMinutesError = when {
            intervalMinutes < INTERVAL_MINUTES_MIN -> "Mindestens $INTERVAL_MINUTES_MIN Minute"
            else                                   -> null
        },
        // endHour zuerst auswerten – Startzeit-Prüfung baut darauf auf
        endHourError = when {
            endHour !in HOUR_MIN..HOUR_MAX -> "Stunde zwischen $HOUR_MIN und $HOUR_MAX"
            else                           -> null
        },
        weekdayStartHourError = when {
            weekdayStartHour !in HOUR_MIN..HOUR_MAX     -> "Stunde zwischen $HOUR_MIN und $HOUR_MAX"
            endHour in HOUR_MIN..HOUR_MAX &&
                weekdayStartHour >= endHour             -> "Muss vor der Endzeit ($endHour Uhr) liegen"
            else                                        -> null
        },
        weekendStartHourError = when {
            weekendStartHour !in HOUR_MIN..HOUR_MAX     -> "Stunde zwischen $HOUR_MIN und $HOUR_MAX"
            endHour in HOUR_MIN..HOUR_MAX &&
                weekendStartHour >= endHour             -> "Muss vor der Endzeit ($endHour Uhr) liegen"
            else                                        -> null
        }
    )
}
