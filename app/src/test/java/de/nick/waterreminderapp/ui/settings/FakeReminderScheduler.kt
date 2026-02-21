package de.nick.waterreminderapp.ui.settings

import de.nick.waterreminderapp.scheduler.IReminderScheduler

/** Aufzeichnender Fake – kein WorkManager nötig. */
class FakeReminderScheduler : IReminderScheduler {
    val startCalls  = mutableListOf<Long>()   // aufgezeichnete intervalMinutes pro start()-Aufruf
    var stopCalled  = false

    override fun start(intervalMinutes: Long) { startCalls.add(intervalMinutes) }
    override fun stop()                       { stopCalled = true }
}

