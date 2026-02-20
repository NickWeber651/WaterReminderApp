package de.nick.waterreminderapp.notification

class FakeNotificationSender : NotificationSender {
    data class ReminderCall(val reminderId: Long, val allowSnooze: Boolean)

    val reminderCalls  = mutableListOf<ReminderCall>()
    var congratsCalled = 0

    override fun sendReminder(reminderId: Long, allowSnooze: Boolean): Boolean {
        reminderCalls.add(ReminderCall(reminderId, allowSnooze))
        return true
    }

    override fun sendCongrats(): Boolean {
        congratsCalled++
        return true
    }

    fun reset() {
        reminderCalls.clear()
        congratsCalled = 0
    }
}

