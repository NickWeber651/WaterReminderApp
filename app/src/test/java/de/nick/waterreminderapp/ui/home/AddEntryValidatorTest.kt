package de.nick.waterreminderapp.ui.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Unit-Tests für [AddEntryValidator] – kein Android, kein Compose nötig.
 */
class AddEntryValidatorTest {

    @Test
    fun `gueltige Menge liefert kein Error`() {
        assertNull(AddEntryValidator.validate("250"))
    }

    @Test
    fun `gueltige Menge 1 liefert kein Error`() {
        assertNull(AddEntryValidator.validate("1"))
    }

    @Test
    fun `sehr grosse Menge ist gueltig`() {
        assertNull(AddEntryValidator.validate("9999"))
    }

    @Test
    fun `null-artige leere Eingabe liefert Fehlermeldung`() {
        assertEquals(AddEntryValidator.ERROR_MESSAGE, AddEntryValidator.validate(""))
    }

    @Test
    fun `Leerzeichen-Eingabe liefert Fehlermeldung`() {
        assertEquals(AddEntryValidator.ERROR_MESSAGE, AddEntryValidator.validate("   "))
    }

    @Test
    fun `Null liefert Fehlermeldung`() {
        assertEquals(AddEntryValidator.ERROR_MESSAGE, AddEntryValidator.validate("0"))
    }

    @Test
    fun `negativer Wert liefert Fehlermeldung`() {
        assertEquals(AddEntryValidator.ERROR_MESSAGE, AddEntryValidator.validate("-1"))
    }

    @Test
    fun `Dezimalzahl liefert Fehlermeldung`() {
        assertEquals(AddEntryValidator.ERROR_MESSAGE, AddEntryValidator.validate("2.5"))
    }

    @Test
    fun `Buchstaben-Eingabe liefert Fehlermeldung`() {
        assertEquals(AddEntryValidator.ERROR_MESSAGE, AddEntryValidator.validate("abc"))
    }

    @Test
    fun `Mischung aus Zahlen und Buchstaben liefert Fehlermeldung`() {
        assertEquals(AddEntryValidator.ERROR_MESSAGE, AddEntryValidator.validate("25abc"))
    }

    @Test
    fun `Int Overflow liefert Fehlermeldung`() {
        // Int.MAX_VALUE + 1 als String → toIntOrNull() gibt null zurück
        val overflow = (Int.MAX_VALUE.toLong() + 1).toString()
        assertEquals(AddEntryValidator.ERROR_MESSAGE, AddEntryValidator.validate(overflow))
    }

    @Test
    fun `parse gibt korrekten Int-Wert zurueck`() {
        assertEquals(250, AddEntryValidator.parse("250"))
        assertEquals(500, AddEntryValidator.parse("500"))
        assertEquals(1, AddEntryValidator.parse("1"))
    }

    @Test
    fun `parse trimmt Leerzeichen`() {
        assertEquals(300, AddEntryValidator.parse("  300  "))
    }
}

