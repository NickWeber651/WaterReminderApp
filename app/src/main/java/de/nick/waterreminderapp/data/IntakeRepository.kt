package de.nick.waterreminderapp.data

import kotlinx.coroutines.flow.Flow

/**
 * Abstraktionsschicht für die Tages-Einträge des Nutzers.
 *
 * Warum ein Interface?
 * Dasselbe Pattern wie [ISettingsStore]: Das Interface erlaubt eine
 * [FakeIntakeRepository]-Implementierung in Unit-Tests, ohne Android-Context
 * oder DataStore zu benötigen. Das ViewModel hängt nur von diesem Interface ab.
 *
 * Tagesreset-Strategie:
 * Die Implementierung ist dafür zuständig, bei einem Tageswechsel die Liste
 * automatisch zu leeren. Die höhere Schicht (ViewModel) muss das nicht wissen.
 */
interface IntakeRepository {

    /**
     * Liefert die heutigen Einträge als reaktiven Flow.
     * Bei Tageswechsel emittiert der Flow automatisch eine leere Liste.
     */
    val todayEntriesFlow: Flow<List<WaterEntry>>

    /**
     * Fügt einen neuen Eintrag zur heutigen Liste hinzu.
     * [amountMl] muss > 0 sein – die Validierung liegt beim Aufrufer (ViewModel).
     */
    suspend fun addEntry(amountMl: Int)

    /**
     * Löscht den Eintrag mit der gegebenen [id] aus der heutigen Liste.
     * Ist die ID nicht vorhanden, passiert nichts (no-op).
     */
    suspend fun removeEntry(id: String)

    /**
     * Bequemlichkeitsfunktion: Summe aller heutigen Einträge in ml.
     * Wird aus [todayEntriesFlow] abgeleitet – kein separater Datenbankwert.
     */
    val totalMlTodayFlow: Flow<Int>

    // ── Congrats-Logik (Tagesziel-Glückwunsch) ───────────────────────────

    /**
     * Ob heute bereits eine Glückwunsch-Notification angezeigt wurde.
     * Wird beim Tageswechsel automatisch auf false zurückgesetzt.
     */
    suspend fun isCongratsSentToday(): Boolean

    /**
     * Markiert, dass heute die Glückwunsch-Notification angezeigt wurde.
     * So wird sie nicht erneut gesendet, wenn der Worker erneut läuft.
     */
    suspend fun markCongratsSent()
}

