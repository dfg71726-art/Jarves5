package com.example.jarvis

import android.app.Notification
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.app.RemoteInput
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Notification access is opt-in. Notification text and reply actions stay on-device in memory. */
class JarvisNotificationListener : NotificationListenerService() {
    override fun onListenerConnected() {
        super.onListenerConnected()
        activeNotifications?.forEach(::publish)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn != null) publish(sbn)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        if (sbn != null) NotificationBridge.remove(sbn.key)
    }

    private fun publish(sbn: StatusBarNotification) {
        if (sbn.packageName == packageName) return
        val notification = sbn.notification ?: return
        val extras = notification.extras ?: Bundle()
        val title = (extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
            ?: extras.getCharSequence(Notification.EXTRA_TITLE_BIG)?.toString().orEmpty()).take(120)
        val body = (extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
            ?: extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()).take(500)
        if (title.isBlank() && body.isBlank()) return
        val appLabel = try {
            packageManager.getApplicationLabel(packageManager.getApplicationInfo(sbn.packageName, 0)).toString()
        } catch (_: Exception) { sbn.packageName }
        val reply = notification.actions
            ?.firstNotNullOfOrNull { action ->
                val inputs = action.remoteInputs?.filter { it.allowFreeFormInput }?.toTypedArray()
                val pendingIntent = action.actionIntent
                if (inputs.isNullOrEmpty() || pendingIntent == null) null
                else NotificationReplyTarget(pendingIntent, inputs, action.title?.toString() ?: "Ответить")
            }
        NotificationBridge.upsert(
            JarvisNotification(sbn.key, sbn.packageName, appLabel, title, body, sbn.postTime, reply != null),
            reply
        )
    }
}

data class JarvisNotification(
    val key: String,
    val packageName: String,
    val appLabel: String,
    val title: String,
    val text: String,
    val postedAt: Long,
    val canReply: Boolean
)

internal data class NotificationReplyTarget(
    val pendingIntent: PendingIntent,
    val remoteInputs: Array<RemoteInput>,
    val actionLabel: String
)

object NotificationBridge {
    private val _notifications = MutableStateFlow<List<JarvisNotification>>(emptyList())
    val notifications: StateFlow<List<JarvisNotification>> = _notifications.asStateFlow()
    private val replyTargets = mutableMapOf<String, NotificationReplyTarget>()

    @Synchronized internal fun upsert(item: JarvisNotification, reply: NotificationReplyTarget?) {
        _notifications.value = (listOf(item) + _notifications.value.filterNot { it.key == item.key })
            .sortedByDescending { it.postedAt }.take(40)
        if (reply != null) replyTargets[item.key] = reply else replyTargets.remove(item.key)
    }

    @Synchronized internal fun remove(key: String) {
        _notifications.value = _notifications.value.filterNot { it.key == key }
        replyTargets.remove(key)
    }

    fun hasAccess(context: Context): Boolean {
        val expected = ComponentName(context, JarvisNotificationListener::class.java).flattenToString()
        val enabled = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners") ?: return false
        return enabled.split(':').any { it.equals(expected, ignoreCase = true) }
    }

    fun openAccessSettings(context: Context) {
        try {
            context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: Exception) { }
    }

    @Synchronized fun sendReply(context: Context, key: String, message: String): Boolean {
        if (message.isBlank()) return false
        val target = replyTargets[key] ?: return false
        return try {
            val fillIn = Intent()
            val results = Bundle()
            target.remoteInputs.forEach { results.putCharSequence(it.resultKey, message) }
            RemoteInput.addResultsToIntent(target.remoteInputs, fillIn, results)
            target.pendingIntent.send(context, 0, fillIn)
            true
        } catch (_: Exception) { false }
    }
}
