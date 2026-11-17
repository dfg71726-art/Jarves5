package com.example.jarvis

import android.content.ComponentName
import android.content.Context
import android.media.session.MediaSessionManager
import android.provider.Settings
import android.content.Intent


class MusicController(private val context: Context) {
    fun hasNotificationAccess(): Boolean {
        val component = ComponentName(context, JarvisNotificationListener::class.java)
        val enabled = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
        return enabled?.contains(component.flattenToString()) == true
    }

    fun openAccessSettings() {
        context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    fun perform(action: String): String {
        if (!hasNotificationAccess()) {
            openAccessSettings()
            return "Для управления активным плеером разреши JARVIS доступ к уведомлениям. Это системное разрешение Android."
        }
        return try {
            val manager = context.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
            val sessions = manager.getActiveSessions(ComponentName(context, JarvisNotificationListener::class.java))
            val controller = sessions.firstOrNull()
                ?: return "Сейчас нет активного медиаплеера. Сначала запусти музыку."
            when (action) {
                "play" -> controller.transportControls.play()
                "pause" -> controller.transportControls.pause()
                "next" -> controller.transportControls.skipToNext()
                "previous" -> controller.transportControls.skipToPrevious()
            }
            when (action) {
                "play" -> "Воспроизведение запрошено."
                "pause" -> "Пауза запрошена."
                "next" -> "Переключаю на следующий трек."
                else -> "Переключаю на предыдущий трек."
            }
        } catch (_: SecurityException) {
            "Android пока не разрешил доступ к медиасессиям. Проверь доступ к уведомлениям для JARVIS."
        } catch (e: Exception) {
            "Не удалось управлять плеером: ${e.localizedMessage ?: "ошибка"}"
        }
    }
}
