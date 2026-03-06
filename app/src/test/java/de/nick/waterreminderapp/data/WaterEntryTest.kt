package de.nick.waterreminderapp.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Reine Unit-Tests für [WaterEntry] – kein Android-Context nötig.
 */
class WaterEntryTest {

    @Test
    fun `create erzeugt Eintrag mit korrekter Menge`() {
        val entry = WaterEntry.create(250)
        assertEquals(250, entry.amountMl)
    }

    @Test
    fun `create erzeugt nicht-leere ID`() {
        val entry = WaterEntry.create(300)
        assertTrue("ID darf nicht leer sein", entry.id.isNotBlank())
    }

    @Test
    fun `create erzeugt eindeutige IDs auch ohne Pause`() {
        // UUID-basierte IDs dürfen nie kollidieren – auch bei sofortigem Aufruf.
        val a = WaterEntry.create(250)
        val b = WaterEntry.create(250)
        assertNotEquals("Zwei Einträge sollen unterschiedliche IDs haben", a.id, b.id)
    }

    @Test
    fun `create erzeugt 100 eindeutige IDs in Schleife`() {
        val ids = (1..100).map { WaterEntry.create(250).id }.toSet()
        assertEquals("Alle 100 IDs müssen eindeutig sein", 100, ids.size)
    }

    @Test
    fun `data class copy erhält ID`() {
        val original = WaterEntry.create(200)
        val copy = original.copy(amountMl = 400)
        assertEquals(original.id, copy.id)
        assertEquals(400, copy.amountMl)
    }
}

