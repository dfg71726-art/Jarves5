package com.example.jarvis

import android.content.Context
import android.content.Intent
import android.net.Uri
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LocalCommandRouter(
    private val context: Context,
    private val onShowApps: (Boolean) -> Unit,
    private val onAccessibilityActionRequest: (String, String, String) -> Unit,
    private val onShowNotifications: () -> Unit = {},
    private val onShowRoutines: () -> Unit = {},
    private val onShowStandby: () -> Unit = {}
) {
    private val music = MusicController(context)
    private val apps = AppCatalog(context)

    fun execute(raw: String): String? {
        val command = raw.trim().lowercase(Locale.forLanguageTag("ru-RU"))
        if (command.isBlank()) return null
        if (command in listOf("отмени действие", "отмени команду", "отмена действия")) {
            AccessibilityBridge.cancelPendingClick()
            return "Подготовленное действие отменено."
        }

        if (listOf("который час", "сколько времени", "текущее время").any(command::contains)) {
            return "Сейчас ${SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())}."
        }
        if (listOf("какое сегодня число", "какая сегодня дата", "сегодняшняя дата").any(command::contains)) {
            return "Сегодня ${SimpleDateFormat("d MMMM yyyy", Locale("ru")).format(Date())}."
        }
        if (command in listOf("помощь", "что ты умеешь", "список команд")) {
            return "Я могу открывать приложения, показывать каталог и избранное, сохранять приложения в избранное командами «добавь Telegram в избранное» и «убери Telegram из избранного», открывать экран ожидания JARVIS, управлять музыкой и готовить подтверждённые нажатия видимых кнопок."
        }

        if (command in setOf("экран ожидания", "режим ожидания", "открой экран ожидания", "экран блокировки jarvis", "режим заставки")) {
            onShowStandby()
            return "Открываю экран ожидания JARVIS. Системная блокировка Android остаётся без изменений."
        }

        val addFavoriteTarget = when {
            command.startsWith("добавь в избранное ") -> command.removePrefix("добавь в избранное ").trim()
            command.startsWith("добавь ") && command.endsWith(" в избранное") -> command.removePrefix("добавь ").removeSuffix(" в избранное").trim()
            else -> null
        }
        if (!addFavoriteTarget.isNullOrBlank()) {
            val app = apps.findApp(addFavoriteTarget)
                ?: return "Не нашёл приложение «$addFavoriteTarget». Скажи «покажи приложения», чтобы открыть каталог."
            AppFavoritesStore.setFavorite(context, app.packageName, true)
            return "${app.label} добавлено в избранное."
        }

        val removeFavoriteTarget = when {
            command.startsWith("убери из избранного ") -> command.removePrefix("убери из избранного ").trim()
            command.startsWith("убери ") && command.endsWith(" из избранного") -> command.removePrefix("убери ").removeSuffix(" из избранного").trim()
            else -> null
        }
        if (!removeFavoriteTarget.isNullOrBlank()) {
            val app = apps.findApp(removeFavoriteTarget)
                ?: return "Не нашёл приложение «$removeFavoriteTarget»."
            AppFavoritesStore.setFavorite(context, app.packageName, false)
            return "${app.label} удалено из избранного."
        }

        Regex("в приложении\\s+(.+?)\\s+(?:нажми кнопку|нажми|выбери пункт)\\s+(.+)").find(command)?.let { match ->
            val appQuery = match.groupValues[1].trim().trim('"', '«', '»', '\'')
            val target = match.groupValues[2].trim().trim('"', '«', '»', '\'')
            if (target.isBlank() || target.length > 80) return "Назови короткую подпись кнопки или пункта меню."
            if (!AccessibilityBridge.isEnabled(context)) {
                return "Для управления элементами внутри приложений сначала включи службу JARVIS в настройках специальных возможностей Android."
            }
            val app = apps.findApp(appQuery)
                ?: return "Не нашёл приложение «$appQuery». Скажи «покажи приложения», чтобы открыть каталог."
            onAccessibilityActionRequest(target, app.packageName, app.label)
            return "Запрос на нажатие «$target» в приложении ${app.label} подготовлен к подтверждению."
        }

        if (listOf("покажи избранные приложения", "открой избранное", "мои избранные приложения", "избранные приложения").any(command::contains)) {
            onShowApps(true)
            return "Открываю избранные приложения."
        }
        if (listOf("покажи приложения", "список приложений", "какие приложения установлены", "открой список приложений", "мои приложения", "управление приложениями").any(command::contains)) {
            onShowApps(false)
            return "Открываю каталог установленных приложений."
        }
        if (command in setOf("покажи уведомления", "открой центр уведомлений", "центр уведомлений", "уведомления jarvis", "открой мои уведомления")) {
            onShowNotifications()
            return "Открываю центр уведомлений."
        }
        if (command in setOf("покажи сценарии", "открой сценарии", "мои сценарии", "управление сценариями")) {
            onShowRoutines()
            return "Открываю персональные сценарии."
        }

        extractTarget(command, listOf("настройки приложения ", "открой настройки приложения ", "информация о приложении ", "инфо приложения "))?.let { target ->
            val app = apps.findApp(target)
                ?: return "Не нашёл приложение «$target». Скажи «покажи приложения», чтобы открыть каталог."
            return if (apps.openAppDetails(app)) "Открываю настройки приложения ${app.label}." else "Не удалось открыть настройки ${app.label}."
        }

        extractTarget(command, listOf("уведомления приложения ", "открой уведомления приложения ", "уведомления для "))?.let { target ->
            val app = apps.findApp(target)
                ?: return "Не нашёл приложение «$target». Скажи «покажи приложения», чтобы открыть каталог."
            return if (apps.openNotificationSettings(app)) "Открываю настройки уведомлений ${app.label}." else "Не удалось открыть настройки уведомлений ${app.label}."
        }

        extractTarget(command, listOf("удали приложение ", "удалить приложение "))?.let { target ->
            val app = apps.findApp(target)
                ?: return "Не нашёл приложение «$target». Скажи «покажи приложения», чтобы открыть каталог."
            return if (apps.requestUninstall(app)) "Открываю системное подтверждение удаления ${app.label}. Удаление произойдёт только после подтверждения на экране Android." else "Android не разрешил открыть удаление ${app.label}."
        }

        extractTarget(command, listOf("закрой приложение ", "останови приложение ", "закрой ", "останови "))?.let { target ->
            val app = apps.findApp(target)
            if (app != null) {
                return if (apps.openAppDetails(app)) {
                    "Android не позволяет JARVIS принудительно закрывать другое приложение. Открыл карточку ${app.label}; при необходимости останови его вручную в системных настройках."
                } else {
                    "Не удалось открыть настройки ${app.label}. Android ограничивает принудительное закрытие других приложений."
                }
            }
        }

        if (command.contains("пауза") || command.contains("останови музыку") || command.contains("выключи музыку")) {
            return music.perform("pause")
        }
        if (command.contains("следующий трек") || command.contains("следующая песня")) return music.perform("next")
        if (command.contains("предыдущий трек") || command.contains("предыдущая песня")) return music.perform("previous")
        if (command.contains("продолжи музыку") || command.contains("включи воспроизведение")) return music.perform("play")
        if (command.startsWith("поиск ") || command.startsWith("найди в интернете ")) {
            val query = command.removePrefix("найди в интернете ").removePrefix("поиск ").trim()
            if (query.isBlank()) return "Скажи, что нужно найти."
            val intent = Intent(Intent.ACTION_WEB_SEARCH).putExtra("query", query).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            return try {
                context.startActivity(intent)
                "Открываю поиск: $query"
            } catch (_: Exception) {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=" + Uri.encode(query))).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                "Открываю результаты поиска."
            }
        }
        if (command.startsWith("открой ") || command.startsWith("запусти ")) {
            var target = command.removePrefix("открой ").removePrefix("запусти ").trim()
            target = target.removePrefix("приложение ").trim()
            if (target.isBlank()) return "Назови приложение, которое нужно открыть."
            return launchApp(target)
        }
        if (command.contains("включи музыку") || command == "музыка") {
            return "Открой свой музыкальный плеер, затем я смогу управлять воспроизведением."
        }
        return null
    }

    private fun launchApp(target: String): String {
        val app = apps.findApp(target)
            ?: return "Не нашёл приложение «$target». Скажи «покажи приложения», чтобы увидеть доступный список."
        return if (apps.launch(app)) "Открываю ${app.label}." else "Не удалось запустить ${app.label}."
    }

    private fun extractTarget(command: String, prefixes: List<String>): String? {
        val prefix = prefixes.firstOrNull(command::startsWith) ?: return null
        return command.removePrefix(prefix).trim().trim('"', '«', '»', '\'')
            .removePrefix("приложение ").trim()
            .takeIf { it.isNotBlank() }
    }
}
