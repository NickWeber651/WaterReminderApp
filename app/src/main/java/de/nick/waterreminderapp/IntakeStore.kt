package de.nick.waterreminderapp

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.Calendar

// ---------------------------------------------------------------------------
// 1. Eigener DataStore – getrennt von "settings", heißt "intake"
//    → jede DataStore-Datei hat ihren eigenen Namen
// ---------------------------------------------------------------------------
private val Context.intakeStore: DataStore<Preferences> by preferencesDataStore(name = "intake")

// ---------------------------------------------------------------------------
// 2. IntakeStore – verwaltet den heutigen Trinkfortschritt
// ---------------------------------------------------------------------------
class IntakeStore(private val context: Context) {

    // -----------------------------------------------------------------------
    // 2a. Keys
    //     TOTAL_ML_TODAY  → Milliliter die heute bereits getrunken wurden
    //     SAVED_DAY       → Calendar.DAY_OF_YEAR des letzten Speichertags
    //                       (Int, 1–366) – kompatibel mit minSdk 24
    //     CONGRATS_SENT   → wurde das Glückwunsch-Event heute schon gesendet?
    //     CONGRATS_DAY    → DAY_OF_YEAR an dem CONGRATS_SENT gesetzt wurde
    // -----------------------------------------------------------------------
    private companion object Keys {
        val TOTAL_ML_TODAY  = intPreferencesKey("total_ml_today")
        val SAVED_DAY       = intPreferencesKey("saved_day")
        val CONGRATS_SENT   = booleanPreferencesKey("congrats_sent")
        val CONGRATS_DAY    = intPreferencesKey("congrats_day")
    }

    // -----------------------------------------------------------------------
    // Hilfsfunktion: aktuellen DAY_OF_YEAR liefern
    // Calendar.getInstance() funktioniert ab API 1 → sicher für minSdk 24
    // DAY_OF_YEAR ist 1-366 und ändert sich täglich → perfekter Tages-Reset
    // -----------------------------------------------------------------------
    private fun todayDayOfYear(): Int = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)

    // -----------------------------------------------------------------------
    // 2b. totalMlTodayFlow – reaktiver Datenstrom des heutigen Trinkwerts
    //
    //     Beim Lesen wird geprüft:
    //       • Ist SAVED_DAY != heute? → neuer Tag → Wert auf 0 zurücksetzen
    //       • Sonst → gespeicherten Wert zurückgeben
    //
    //     WICHTIG: Das map-Lambda darf NICHT schreiben (nur lesen).
    //     Der Reset beim Lesen ist ein "virtueller" Reset im Flow –
    //     der tatsächliche Schreibreset passiert in addMl() und markCongratsSent().
    // -----------------------------------------------------------------------
    val totalMlTodayFlow: Flow<Int> = context.intakeStore.data.map { prefs ->
        val savedDay = prefs[SAVED_DAY] ?: 0
        if (savedDay != todayDayOfYear()) {
            0   // neuer Tag → virtuell 0 liefern
        } else {
            prefs[TOTAL_ML_TODAY] ?: 0
        }
    }

    // -----------------------------------------------------------------------
    // 2c. addMl – fügt Milliliter zum heutigen Gesamtwert hinzu
    //
    //     Ablauf:
    //       1. Prüfe ob SAVED_DAY != heute → wenn ja, reset auf 0 (neuer Tag)
    //       2. Addiere ml zum aktuellen Wert
    //       3. Schreibe neuen Wert und aktuellen Tag zurück
    //
    //     dataStore.edit { } ist atomar → kein Datenverlust bei parallelen Zugriffen
    // -----------------------------------------------------------------------
    suspend fun addMl(ml: Int) {
        context.intakeStore.edit { prefs ->
            val today = todayDayOfYear()
            val savedDay = prefs[SAVED_DAY] ?: 0

            // Tageswechsel → Werte zurücksetzen
            val currentTotal = if (savedDay != today) 0 else (prefs[TOTAL_ML_TODAY] ?: 0)

            prefs[TOTAL_ML_TODAY] = currentTotal + ml
            prefs[SAVED_DAY]      = today
        }
    }

    // -----------------------------------------------------------------------
    // 2d. hasReachedGoal – prüft ob das Tagesziel erreicht wurde
    //
    //     suspend fun weil wir einmalig aus dem DataStore lesen müssen.
    //     kotlinx.coroutines.flow.first() holt genau einen Wert aus dem Flow
    //     und gibt ihn zurück – kein dauerhaftes Beobachten nötig.
    // -----------------------------------------------------------------------
    suspend fun hasReachedGoal(goalMl: Int): Boolean {
        val current = totalMlTodayFlow.first()
        return current >= goalMl
    }

    // -----------------------------------------------------------------------
    // 2e. markCongratsSent – speichert dass der Glückwunsch heute gesendet wurde
    //
    //     Wir speichern zusätzlich CONGRATS_DAY = heute, damit wir beim
    //     nächsten Tag automatisch wissen dass CONGRATS_SENT veraltet ist.
    // -----------------------------------------------------------------------
    suspend fun markCongratsSent() {
        context.intakeStore.edit { prefs ->
            prefs[CONGRATS_SENT] = true
            prefs[CONGRATS_DAY]  = todayDayOfYear()
        }
    }

    // -----------------------------------------------------------------------
    // 2f. congratsSentTodayFlow – liefert true nur wenn Glückwunsch
    //     HEUTE schon gesendet wurde (automatischer Tages-Reset)
    // -----------------------------------------------------------------------
    val congratsSentTodayFlow: Flow<Boolean> = context.intakeStore.data.map { prefs ->
        val savedDay = prefs[CONGRATS_DAY] ?: 0
        if (savedDay != todayDayOfYear()) {
            false   // neuer Tag → Glückwunsch noch nicht gesendet
        } else {
            prefs[CONGRATS_SENT] ?: false
        }
    }

    // -----------------------------------------------------------------------
    // 2g. isCongratsSentToday – einmalige suspend-Abfrage (kein Flow nötig)
    //     Liest genau einen Wert aus congratsSentTodayFlow.
    //     Praktisch wenn kein dauerhaftes Beobachten gewünscht ist,
    //     z.B. in BroadcastReceiver oder Worker.
    // -----------------------------------------------------------------------
    suspend fun isCongratsSentToday(): Boolean = congratsSentTodayFlow.first()
}

