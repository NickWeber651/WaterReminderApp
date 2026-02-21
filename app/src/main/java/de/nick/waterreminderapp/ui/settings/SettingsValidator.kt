package de.nick.waterreminderapp.ui.settings

/**
 * Zustandslose Validierungs-Logik für den Settings-Screen.
 *
 * Regeln:
 *   - goalMl:          >= 0  (0 ml ist gültig, negativ nicht)
 *   - intervalMinutes: >= 1  (0 oder negativ wäre eine Endlosschleife)
 *   - hours:           0..23 (Uhrzeit-Bereich)
 *   - endHour > startHour:   Warnung, damit das Zeitfenster Sinn ergibt
 *
 * Bewusst ohne Android-Abhängigkeiten – direkt in JVM Unit-Tests testbar.
 */
object SettingsValidator {

    const val GOAL_ML_MIN          = 0
    const val INTERVAL_MINUTES_MIN = 1   // < 15 min → OneTimeWorkRequest-Modus, >= 15 min → PeriodicWorkRequest
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
            goalMl < GOAL_ML_MIN             -> "Darf nicht negativ sein"
            else                             -> null
        },
        intervalMinutesError = when {
            intervalMinutes < INTERVAL_MINUTES_MIN -> "Mindestens $INTERVAL_MINUTES_MIN Minute"
            else                                   -> null
        },
        endHourError = when {
            endHour !in HOUR_MIN..HOUR_MAX -> "Stunde zwischen $HOUR_MIN und $HOUR_MAX"
            else                           -> null
        },
        weekdayStartHourError = when {
            weekdayStartHour !in HOUR_MIN..HOUR_MAX        -> "Stunde zwischen $HOUR_MIN und $HOUR_MAX"
            weekdayStartHour >= endHour                    -> "Muss vor Endstunde liegen"
            else                                           -> null
        },
        weekendStartHourError = when {
            weekendStartHour !in HOUR_MIN..HOUR_MAX        -> "Stunde zwischen $HOUR_MIN und $HOUR_MAX"
            weekendStartHour >= endHour                    -> "Muss vor Endstunde liegen"
            else                                           -> null
        }
    )
}
