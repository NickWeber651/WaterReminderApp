package de.nick.waterreminderapp.data

/**
 * Repräsentiert einen einzelnen Trinkvorgang des aktuellen Tages.
 *
 * Bewusst minimalistisch gehalten:
 * - Kein Zeitstempel (Anforderung 12)
 * - Kein Getränketyp (Anforderung 11)
 * - Nur Wasser + Menge in ml
 *
 * [id] wird beim Erstellen per [WaterEntry.create] automatisch vergeben und
 * dient ausschließlich als stabiler Schlüssel zum Löschen eines Eintrags.
 *
 * Die Klasse ist bewusst eine reine data class ohne Android-Abhängigkeit,
 * damit sie in einfachen Unit-Tests ohne Robolectric testbar bleibt.
 */
data class WaterEntry(
    val id: String,
    val amountMl: Int
) {
    companion object {
        /**
         * Erzeugt einen neuen Eintrag mit einer eindeutigen ID.
         * Wir verwenden einen einfachen Timestamp-basierten String als ID –
         * ausreichend für eine single-user offline App.
         */
        fun create(amountMl: Int): WaterEntry =
            WaterEntry(
                id       = System.currentTimeMillis().toString(),
                amountMl = amountMl
            )
    }
}

