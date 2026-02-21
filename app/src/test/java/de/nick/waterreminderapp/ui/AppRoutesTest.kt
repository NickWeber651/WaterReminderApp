package de.nick.waterreminderapp.ui

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Einfacher Unit-Test, der sicherstellt, dass die Routen-Konstanten die
 * erwarteten String-Werte haben.
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
    fun homeUndSettingsSindVerschieden() {
        assert(AppRoutes.HOME != AppRoutes.SETTINGS)
    }
}
