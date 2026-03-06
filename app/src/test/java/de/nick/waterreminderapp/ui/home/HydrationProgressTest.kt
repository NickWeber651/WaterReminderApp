package de.nick.waterreminderapp.ui.home

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit-Tests für [calculateProgress].
 *
 * Warum hier Tests?
 * calculateProgress ist reine Kotlin-Logik ohne Android- oder Compose-Abhängigkeit
 * und deckt kritische Edge Cases ab – daher sind schnelle JVM-Tests ideal.
 */
class HydrationProgressTest {

    // -----------------------------------------------------------------------
    // Normalfälle
    // -----------------------------------------------------------------------

    @Test
    fun halbzielErgibt0_5() {
        assertEquals(0.5f, calculateProgress(totalMl = 1250, goalMl = 2500), 0.001f)
    }

    @Test
    fun genauZielErreichtErgibt1_0() {
        assertEquals(1.0f, calculateProgress(totalMl = 2500, goalMl = 2500), 0.001f)
    }

    @Test
    fun einViertelZielErgibt0_25() {
        assertEquals(0.25f, calculateProgress(totalMl = 500, goalMl = 2000), 0.001f)
    }

    // -----------------------------------------------------------------------
    // Edge Cases: Ziel ungültig
    // -----------------------------------------------------------------------

    @Test
    fun goalMlNullErgibt0() {
        assertEquals(0f, calculateProgress(totalMl = 500, goalMl = 0), 0.001f)
    }

    @Test
    fun goalMlNegativErgibt0() {
        assertEquals(0f, calculateProgress(totalMl = 500, goalMl = -100), 0.001f)
    }

    // -----------------------------------------------------------------------
    // Edge Cases: Trinkmenge ungültig
    // -----------------------------------------------------------------------

    @Test
    fun totalMlNullErgibt0() {
        assertEquals(0f, calculateProgress(totalMl = 0, goalMl = 2500), 0.001f)
    }

    @Test
    fun totalMlNegativErgibt0() {
        assertEquals(0f, calculateProgress(totalMl = -100, goalMl = 2500), 0.001f)
    }

    // -----------------------------------------------------------------------
    // Edge Cases: Überschreitung des Ziels
    // -----------------------------------------------------------------------

    @Test
    fun totalMlUeberZielWirdAuf1Geclampt() {
        assertEquals(1.0f, calculateProgress(totalMl = 9999, goalMl = 2500), 0.001f)
    }

    // -----------------------------------------------------------------------
    // Beides null / beide ungültig
    // -----------------------------------------------------------------------

    @Test
    fun beideNullErgibt0() {
        assertEquals(0f, calculateProgress(totalMl = 0, goalMl = 0), 0.001f)
    }
}

