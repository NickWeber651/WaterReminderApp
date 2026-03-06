package de.nick.waterreminderapp.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Stellt sicher, dass die Routen-Konstanten die erwarteten String-Werte haben.
 * So merken wir sofort, wenn jemand eine Route umbenennt, ohne die Navigation anzupassen.
 */
class AppRoutesTest {

    @Test
    fun homeRouteHatKorrektenWert() {
        assertEquals("home", AppRoutes.HOME)
    }

    @Test
    fun settingsRouteHatKorrektenWert() {
        assertEquals("settings", AppRoutes.SETTINGS)
    }

    @Test
    fun historyRouteHatKorrektenWert() {
        assertEquals("history", AppRoutes.HISTORY)
    }

    @Test
    fun alleRoutenSindVerschieden() {
        val routes = listOf(AppRoutes.HOME, AppRoutes.SETTINGS, AppRoutes.HISTORY)
        assertEquals("Alle Routen müssen eindeutig sein", routes.size, routes.distinct().size)
    }
}

