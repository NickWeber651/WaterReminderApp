package de.nick.waterreminderapp.scheduler

/**
 * Abstraktionsschicht für den ReminderScheduler.
 * Ermöglicht Fake-Implementierungen in Unit-Tests ohne WorkManager/Context.
 */
interface IReminderScheduler {
    fun start(intervalMinutes: Long)
    fun stop()
}

