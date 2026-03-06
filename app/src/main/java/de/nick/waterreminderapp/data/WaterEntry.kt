package de.nick.waterreminderapp.data

import java.util.UUID

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
         * UUID statt Timestamp, damit auch bei schnellen Aufrufen
         * (z.B. repeat(3) { addEntry(250) }) keine ID-Kollision entsteht.
         */
        fun create(amountMl: Int): WaterEntry =
            WaterEntry(
                id       = UUID.randomUUID().toString(),
                amountMl = amountMl
            )
    }
}

