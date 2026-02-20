package de.nick.waterreminderapp.notification

interface NotificationSender {
    fun sendReminder(reminderId: Long, allowSnooze: Boolean): Boolean
    fun sendCongrats(): Boolean
}

class RealNotificationSender(private val helper: NotificationHelper) : NotificationSender {
    override fun sendReminder(reminderId: Long, allowSnooze: Boolean) =
        helper.showReminderNotification(reminderId, allowSnooze)
    override fun sendCongrats() =
        helper.showCongratsNotification()
}

