package de.nick.waterreminderapp

/**
 * Abstrahiert das Senden von Notifications.
 * → Produktionscode nutzt RealNotificationSender (delegiert an NotificationHelper).
 * → Tests nutzen FakeNotificationSender, der nur Aufrufe aufzeichnet.
 */
interface NotificationSender {
    fun sendReminder(reminderId: Long, allowSnooze: Boolean): Boolean
    fun sendCongrats(): Boolean
}

/** Echtzeit-Implementierung – delegiert an NotificationHelper */
class RealNotificationSender(private val helper: NotificationHelper) : NotificationSender {
    override fun sendReminder(reminderId: Long, allowSnooze: Boolean) =
        helper.showReminderNotification(reminderId, allowSnooze)

    override fun sendCongrats() =
        helper.showCongratsNotification()
}

