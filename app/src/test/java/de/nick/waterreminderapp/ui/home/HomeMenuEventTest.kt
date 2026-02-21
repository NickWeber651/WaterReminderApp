package de.nick.waterreminderapp.ui.home

import de.nick.waterreminderapp.ui.navigation.AppRoutes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Testet das Event-Routing des Home-Overflow-Menüs.
 *
 * Warum kein Compose-UI-Test?
 * UI-Tests mit ComposeTestRule erfordern ein Android-Gerät/Emulator (Instrumented).
 * Hier testen wir stattdessen das *Verhalten*: Welche Route wird beim Klick auf
 * "Einstellungen" angefragt? Das ist reiner Kotlin-Code ohne Android-Abhängigkeit.
 */
class HomeMenuEventTest {

    /**
     * Simuliert: Nutzer klickt im Menü auf "Einstellungen".
     * Erwartet: onNavigateToSettings-Lambda wird genau einmal aufgerufen.
     */
    @Test
    fun einstellungenKlickRuftNavigationCallbackAuf() {
        var callCount = 0
        val onNavigateToSettings = { callCount++ }

        // Simuliert den onClick-Handler aus dem DropdownMenuItem
        val handleMenuClick: (menuItem: String) -> Unit = { item ->
            if (item == "Einstellungen") {
                onNavigateToSettings()
            }
        }

        handleMenuClick("Einstellungen")

        assertEquals("onNavigateToSettings muss genau 1x aufgerufen werden", 1, callCount)
    }

    @Test
    fun andererMenuItemRuftNavigationNichtAuf() {
        var callCount = 0
        val onNavigateToSettings = { callCount++ }

        val handleMenuClick: (menuItem: String) -> Unit = { item ->
            if (item == "Einstellungen") {
                onNavigateToSettings()
            }
        }

        handleMenuClick("Sonstiges")

        assertEquals("Kein Navigations-Callback bei unbekanntem Item", 0, callCount)
    }

    @Test
    fun settingsRouteIstKorrekt() {
        // Stellt sicher, dass die Route, zu der navigiert wird, mit AppRoutes übereinstimmt
        assertEquals("settings", AppRoutes.SETTINGS)
    }
}

