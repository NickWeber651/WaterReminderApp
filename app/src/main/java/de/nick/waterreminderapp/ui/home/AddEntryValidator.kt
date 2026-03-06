package de.nick.waterreminderapp.ui.home

/**
 * Zustandslose Validierungslogik für die Wassermenge-Eingabe.
 *
 * Bewusst als eigenständiges Objekt – kein Android, keine Compose-Abhängigkeit,
 * direkt in einfachen JVM-Unit-Tests testbar.
 *
 * Regeln (Anforderungen 7 + 8):
 *   - Eingabe muss eine gültige ganze Zahl sein
 *   - Wert muss > 0 sein
 *   - Alles andere → "Ungültiger Wert"
 */
object AddEntryValidator {

    const val ERROR_MESSAGE = "Ungültiger Wert"

    /**
     * Validiert den rohen String aus dem Eingabefeld.
     * Gibt [ERROR_MESSAGE] zurück, wenn ungültig – sonst null.
     */
    fun validate(input: String): String? {
        val value = input.trim().toIntOrNull()
        return if (value == null || value <= 0) ERROR_MESSAGE else null
    }

    /**
     * Parst den validierten Input zu einem Int.
     * Darf nur nach erfolgreicher [validate]-Prüfung aufgerufen werden.
     */
    fun parse(input: String): Int = input.trim().toInt()
}

