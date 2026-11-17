package com.example.jarvis

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import android.widget.Toast
import kotlinx.coroutines.launch
import java.util.Locale

class MainActivity : ComponentActivity() {
    var onIslandVoiceRequest: (() -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (intent.getBooleanExtra(EXTRA_START_LISTENING, false)) {
            getSharedPreferences("jarvis_settings", Context.MODE_PRIVATE).edit().putBoolean("pending_island_voice", true).apply()
        }
        setContent { LegionTheme { JarvisApp() } }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.getBooleanExtra(EXTRA_START_LISTENING, false)) {
            onIslandVoiceRequest?.invoke()
                ?: getSharedPreferences("jarvis_settings", Context.MODE_PRIVATE).edit().putBoolean("pending_island_voice", true).apply()
        }
    }

    companion object {
        const val EXTRA_START_LISTENING = "com.example.jarvis.extra.START_LISTENING"
    }
}

private data class ChatMessage(val text: String, val fromUser: Boolean)

private class JarvisVoice(context: Context) : TextToSpeech.OnInitListener {
    private val tts = TextToSpeech(context.applicationContext, this)
    private val mainHandler = Handler(Looper.getMainLooper())
    @Volatile private var ready = false
    var onSpeakingChanged: ((Boolean) -> Unit)? = null

    override fun onInit(status: Int) {
        ready = status == TextToSpeech.SUCCESS
        if (ready) {
            tts.language = Locale("ru", "RU")
            tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) = mainHandler.post { onSpeakingChanged?.invoke(true) }.let { }
                override fun onDone(utteranceId: String?) = mainHandler.post { onSpeakingChanged?.invoke(false) }.let { }
                @Deprecated("Deprecated in Android API")
                override fun onError(utteranceId: String?) = mainHandler.post { onSpeakingChanged?.invoke(false) }.let { }
                override fun onError(utteranceId: String?, errorCode: Int) = mainHandler.post { onSpeakingChanged?.invoke(false) }.let { }
            })
        }
    }
    fun speak(text: String) {
        if (ready) tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "jarvis-${System.currentTimeMillis()}")
    }
    fun shutdown() { tts.stop(); tts.shutdown() }
}

@Composable
private fun JarvisApp() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("jarvis_settings", Context.MODE_PRIVATE) }
    var appsVisible by remember { mutableStateOf(false) }
    var appsFavoritesOnly by remember { mutableStateOf(false) }
    var notificationsVisible by remember { mutableStateOf(false) }
    var routinesVisible by remember { mutableStateOf(false) }
    var standbyVisible by remember { mutableStateOf(false) }
    var pendingAccessibilityAction by remember { mutableStateOf<String?>(null) }
    var islandEnabled by remember { mutableStateOf(prefs.getBoolean("dynamic_island_enabled", false)) }
    val router = remember {
        LocalCommandRouter(
            context,
            onShowApps = { favoritesOnly -> standbyVisible = false; appsFavoritesOnly = favoritesOnly; appsVisible = true },
            onAccessibilityActionRequest = { label, packageName, appLabel ->
                pendingAccessibilityAction = "$packageName|$appLabel|$label"
            },
            onShowNotifications = { standbyVisible = false; notificationsVisible = true },
            onShowRoutines = { standbyVisible = false; routinesVisible = true },
            onShowStandby = { standbyVisible = true }
        )
    }
    val aiClient = remember { AiClient() }
    val speech = remember { SpeechController(context) }
    val voice = remember { JarvisVoice(context) }
    val scope = rememberCoroutineScope()

    var messages by remember { mutableStateOf(listOf(ChatMessage("Система запущена. Я JARVIS. Чем могу помочь?", false))) }
    var status by remember { mutableStateOf("СИСТЕМА ГОТОВА") }
    var input by rememberSaveable { mutableStateOf("") }
    var onlineMode by rememberSaveable { mutableStateOf(true) }
    var wakeMode by rememberSaveable { mutableStateOf(false) }
    var settingsVisible by remember { mutableStateOf(false) }
    var cameraVisible by remember { mutableStateOf(false) }
    var backendUrl by rememberSaveable { mutableStateOf(prefs.getString("backend_url", "") ?: "") }

    val submitMessage: (String) -> Unit = { raw ->
        val text = raw.trim()
        if (text.isNotBlank()) {
            messages = messages + ChatMessage(text, true)
            input = ""
            status = "АНАЛИЗ ЗАПРОСА"
            scope.launch {
                val localReply = router.execute(text)
                val reply = if (localReply != null) {
                    localReply
                } else if (onlineMode && backendUrl.isNotBlank()) {
                    try { aiClient.ask(backendUrl, text) }
                    catch (e: Exception) { "Не удалось связаться с AI-сервером: ${e.localizedMessage ?: "ошибка сети"}. Локальные команды продолжают работать." }
                } else {
                    "Эта команда требует онлайн-ИИ. Подключи HTTPS-адрес AI-сервера в настройках или используй локальные команды: «открой…», «поиск…», «который час», «пауза», «следующий трек»."
                }
                messages = messages + ChatMessage(reply, false)
                status = "СИСТЕМА ГОТОВА"
                voice.speak(reply)
            }
        }
    }

    SideEffect {
        speech.onText = submitMessage
        speech.onStatus = { status = it }
        speech.onWakeOnly = { status = "АКТИВАЦИЯ JARVIS"; voice.speak("Слушаю") }
        voice.onSpeakingChanged = { speaking -> status = if (speaking) "ОТВЕЧАЮ" else "СИСТЕМА ГОТОВА" }
        if (prefs.getBoolean("dynamic_island_enabled", false)) {
            DynamicIslandService.update(context, status)
        }
    }
    LaunchedEffect(Unit) {
        if (prefs.getBoolean("dynamic_island_enabled", false) && android.provider.Settings.canDrawOverlays(context)) {
            islandEnabled = true
            DynamicIslandService.start(context, status)
        } else if (!android.provider.Settings.canDrawOverlays(context)) {
            islandEnabled = false
        }
    }
    DisposableEffect(speech, voice) {
        onDispose { speech.destroy(); voice.shutdown() }
    }

    val audioPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) speech.start(onDeviceOnly = !onlineMode, keepWakeListening = false)
        else status = "НУЖНО РАЗРЕШЕНИЕ НА МИКРОФОН"
    }

    val startIslandListening: () -> Unit = {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            wakeMode = false
            status = "СЛУШАЮ КОМАНДУ"
            speech.start(onDeviceOnly = !onlineMode, keepWakeListening = false)
        } else audioPermission.launch(Manifest.permission.RECORD_AUDIO)
    }
    SideEffect {
        (context as? MainActivity)?.onIslandVoiceRequest = startIslandListening
        if (prefs.getBoolean("pending_island_voice", false)) {
            prefs.edit().remove("pending_island_voice").apply()
            startIslandListening()
        }
    }

    if (standbyVisible) {
        StandbyScreen(
            onClose = { standbyVisible = false },
            onVoice = startIslandListening,
            onApps = { standbyVisible = false; appsVisible = true; appsFavoritesOnly = false },
            onNotifications = { standbyVisible = false; notificationsVisible = true },
            onRoutines = { standbyVisible = false; routinesVisible = true }
        )
        return
    }
    if (cameraVisible) {
        CameraScannerScreen(onClose = { cameraVisible = false })
        return
    }
    if (appsVisible) {
        AppCatalogScreen(onClose = { appsVisible = false }, initialFavoritesOnly = appsFavoritesOnly)
        return
    }
    if (notificationsVisible) {
        NotificationCenterScreen(onClose = { notificationsVisible = false })
        return
    }
    if (routinesVisible) {
        RoutinesScreen(onClose = { routinesVisible = false })
        return
    }

    LegionBackdrop {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 9.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Text("JARVIS", color = LegionPalette.white, fontSize = 25.sp, fontWeight = FontWeight.Black, letterSpacing = 3.sp)
                    Text("DEDSEC NETWORK  //  PERSONAL AI NODE", color = LegionPalette.muted, fontSize = 7.sp, letterSpacing = 1.1.sp)
                }
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        Box(Modifier.size(6.dp).background(if (onlineMode) LegionPalette.green else LegionPalette.amber, CircleShape))
                        Text(if (onlineMode) "ONLINE LINK" else "OFFLINE CORE", color = if (onlineMode) LegionPalette.green else LegionPalette.amber, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 0.8.sp)
                    }
                    Text(status.uppercase(Locale.getDefault()), color = LegionPalette.muted, fontSize = 7.sp, maxLines = 1)
                }
            }

            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("NETWORK MODE", color = LegionPalette.muted, fontSize = 8.sp, letterSpacing = 1.sp, modifier = Modifier.weight(1f))
                Row(
                    modifier = Modifier.clip(RoundedCornerShape(5.dp)).background(Color(0xCC111B28)).border(1.dp, Color(0xFF244052), RoundedCornerShape(5.dp)).padding(2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ModeChip("ONLINE", onlineMode, LegionPalette.cyan) { onlineMode = true }
                    ModeChip("OFFLINE", !onlineMode, LegionPalette.red) { onlineMode = false }
                }
            }

            Row(Modifier.fillMaxWidth().height(154.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                JarvisCore(isListening = status.contains("СЛУШАЮ") || status.contains("РАСПОЗНАЮ"), modifier = Modifier.size(154.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text("NEURAL INTERFACE", color = LegionPalette.cyan, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.2.sp)
                    Text(if (status.contains("СЛУШАЮ") || status.contains("РАСПОЗНАЮ")) "Я СЛУШАЮ" else "СИСТЕМА ГОТОВА", color = LegionPalette.white, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Text("Скажи «Джарвис» или активируй микрофон", color = LegionPalette.muted, fontSize = 9.sp, lineHeight = 13.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        LegionHudMetric("CORE", "ACTIVE", LegionPalette.green, Modifier.weight(1f))
                        LegionHudMetric("SECURITY", "LOCKED", LegionPalette.red, Modifier.weight(1f))
                    }
                    Text("NODE ID  /  JRV-05-UKR", color = Color(0xFF597184), fontSize = 7.sp, letterSpacing = 0.8.sp)
                }
            }

            LegionSectionLabel("LIVE FEED", "LOCAL SESSION")
            Card(
                modifier = Modifier.fillMaxWidth().weight(1f),
                colors = CardDefaults.cardColors(containerColor = LegionPalette.panel),
                shape = RoundedCornerShape(topStart = 5.dp, topEnd = 16.dp, bottomEnd = 5.dp, bottomStart = 16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF245064))
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(messages.takeLast(30)) { message -> MessageBubble(message) }
                }
            }

            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("ВВЕДИ КОМАНДУ ДЛЯ JARVIS…", color = Color(0xFF71899B), fontSize = 10.sp) },
                    singleLine = true,
                    shape = RoundedCornerShape(topStart = 5.dp, topEnd = 13.dp, bottomEnd = 5.dp, bottomStart = 13.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = LegionPalette.cyan,
                        unfocusedBorderColor = Color(0xFF28475A),
                        cursorColor = LegionPalette.cyan
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { submitMessage(input) })
                )
                Spacer(Modifier.width(7.dp))
                Button(
                    onClick = { submitMessage(input) },
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                    modifier = Modifier.size(52.dp),
                    shape = RoundedCornerShape(9.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = LegionPalette.red)
                ) { Icon(Icons.Default.Send, contentDescription = "Отправить", tint = Color.White) }
            }

            LegionSectionLabel("QUICK ACCESS", "6 MODULES")
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                LegionActionTile("ГОЛОС", "VOICE INPUT", Icons.Default.Mic, LegionPalette.cyan, Modifier.weight(1f)) {
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                        wakeMode = false
                        speech.start(onDeviceOnly = !onlineMode, keepWakeListening = false)
                    } else audioPermission.launch(Manifest.permission.RECORD_AUDIO)
                }
                LegionActionTile("WAKE WORD", if (wakeMode) "АКТИВЕН" else "АКТИВАЦИЯ", Icons.Default.Waves, LegionPalette.red, Modifier.weight(1f)) {
                    if (wakeMode) { wakeMode = false; speech.stop() }
                    else if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                        wakeMode = true
                        speech.start(onDeviceOnly = !onlineMode, keepWakeListening = true)
                    } else audioPermission.launch(Manifest.permission.RECORD_AUDIO)
                }
                LegionActionTile("КАМЕРА", "VISION SCAN", Icons.Default.CameraAlt, LegionPalette.green, Modifier.weight(1f)) { cameraVisible = true }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                LegionActionTile("ПРИЛОЖЕНИЯ", "APP CONTROL", Icons.Default.Apps, LegionPalette.cyan, Modifier.weight(1f)) { appsVisible = true }
                LegionActionTile("МУЗЫКА", "MEDIA LINK", Icons.Default.MusicNote, LegionPalette.amber, Modifier.weight(1f)) {
                    val music = MusicController(context)
                    if (!music.hasNotificationAccess()) music.openAccessSettings()
                    else {
                        val reply = music.perform("play")
                        messages = messages + ChatMessage(reply, false)
                        voice.speak(reply)
                    }
                }
                LegionActionTile("НАСТРОЙКИ", "SYSTEM CONFIG", Icons.Default.Settings, LegionPalette.red, Modifier.weight(1f)) { settingsVisible = true }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { notificationsVisible = true }, contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 5.dp, vertical = 0.dp)) {
                    Icon(Icons.Default.Notifications, contentDescription = null, tint = LegionPalette.cyan, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(4.dp)); Text("УВЕДОМЛЕНИЯ", color = LegionPalette.white, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                }
                TextButton(onClick = { routinesVisible = true }, contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 5.dp, vertical = 0.dp)) {
                    Icon(Icons.Default.ListAlt, contentDescription = null, tint = LegionPalette.red, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(4.dp)); Text("СЦЕНАРИИ", color = LegionPalette.white, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                }
                TextButton(onClick = { standbyVisible = true }, contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 5.dp, vertical = 0.dp)) {
                    Text("STANDBY  ↗", color = LegionPalette.cyan, fontSize = 8.sp, fontWeight = FontWeight.Black)
                }
            }
        }
    }

    if (settingsVisible) {
        AlertDialog(
            onDismissRequest = { settingsVisible = false },
            containerColor = Color(0xFF101B2B),
            title = { Text("JARVIS / НАСТРОЙКИ", color = Color(0xFF00E5FF), fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Адрес AI-сервера (HTTPS)", color = Color.White, fontSize = 13.sp)
                    OutlinedTextField(
                        value = backendUrl,
                        onValueChange = { backendUrl = it },
                        placeholder = { Text("https://your-server.example", color = Color.Gray) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = Color(0xFF00E5FF), unfocusedBorderColor = Color(0xFF345064))
                    )
                    Text("Приложение отправляет запрос на адрес /chat. Сервер должен вернуть JSON: { reply: \"текст ответа\" }. Секретный API-ключ хранится на сервере, а не в APK.", color = Color(0xFF9CB3C4), fontSize = 11.sp)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Онлайн-режим", color = Color.White, modifier = Modifier.weight(1f))
                        Switch(checked = onlineMode, onCheckedChange = { onlineMode = it })
                    }
                    Text("Режим фразы «Джарвис» работает, пока приложение открыто. Для постоянного фонового пробуждения нужна отдельная реализация с foreground service и системными ограничениями Android.", color = Color(0xFFFFC857), fontSize = 10.sp)
                    androidx.compose.material3.HorizontalDivider(color = Color(0xFF294257))
                    Text("УПРАВЛЕНИЕ ПРИЛОЖЕНИЯМИ", color = Color(0xFF00E5FF), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text(
                        if (AccessibilityBridge.isEnabled(context)) "Служба специальных возможностей JARVIS включена. Нажатия выполняются только после подтверждения." else "Служба выключена. Для нажатий по видимым кнопкам Android попросит включить специальное разрешение.",
                        color = Color(0xFF9CB3C4), fontSize = 10.sp
                    )
                    TextButton(onClick = {
                        context.startActivity(Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    }) { Text("ОТКРЫТЬ СПЕЦИАЛЬНЫЕ ВОЗМОЖНОСТИ", color = Color(0xFF00D9F5), fontSize = 10.sp) }
                    androidx.compose.material3.HorizontalDivider(color = Color(0xFF294257))
                    Text("УВЕДОМЛЕНИЯ И ОТВЕТЫ", color = Color(0xFF00E5FF), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text(
                        if (NotificationBridge.hasAccess(context)) "Доступ включён. Уведомления отображаются только в локальном центре JARVIS; ответы отправляются только после отдельного подтверждения." else "Доступ выключен. При желании разреши JARVIS видеть уведомления, чтобы отвечать на поддерживаемые уведомления после подтверждения.",
                        color = Color(0xFF9CB3C4), fontSize = 10.sp
                    )
                    TextButton(onClick = { NotificationBridge.openAccessSettings(context) }) {
                        Text("НАСТРОИТЬ ДОСТУП К УВЕДОМЛЕНИЯМ", color = Color(0xFF00D9F5), fontSize = 10.sp)
                    }
                    androidx.compose.material3.HorizontalDivider(color = Color(0xFF294257))
                    Text("DYNAMIC ISLAND", color = Color(0xFF00E5FF), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text("Плавающий индикатор поверх других приложений. Не использует микрофон или камеру.", color = Color(0xFF9CB3C4), fontSize = 10.sp)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(if (islandEnabled) "Остров включён" else "Остров выключен", color = Color.White, modifier = Modifier.weight(1f), fontSize = 12.sp)
                        Switch(checked = islandEnabled, onCheckedChange = { enabled ->
                            if (enabled) {
                                if (android.provider.Settings.canDrawOverlays(context)) {
                                    prefs.edit().putBoolean("dynamic_island_enabled", true).apply()
                                    islandEnabled = true
                                    DynamicIslandService.start(context, status)
                                } else {
                                    context.startActivity(Intent(android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                                }
                            } else {
                                prefs.edit().putBoolean("dynamic_island_enabled", false).apply()
                                islandEnabled = false
                                DynamicIslandService.stop(context)
                            }
                        })
                    }
                    if (!android.provider.Settings.canDrawOverlays(context)) {
                        Text("Для острова нужно разрешение «Поверх других приложений». После выдачи вернись сюда и включи переключатель.", color = Color(0xFFFFC857), fontSize = 10.sp)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    prefs.edit().putString("backend_url", backendUrl.trim()).apply()
                    settingsVisible = false
                }) { Text("СОХРАНИТЬ", color = Color(0xFF00E5FF)) }
            },
            dismissButton = { TextButton(onClick = { settingsVisible = false }) { Text("ОТМЕНА", color = Color(0xFF9CB3C4)) } }
        )
    }

    pendingAccessibilityAction?.let { queued ->
        val parts = queued.split('|', limit = 3)
        val targetPackage = parts.getOrElse(0) { "" }
        val targetApp = parts.getOrElse(1) { "Приложение" }
        val target = parts.getOrElse(2) { "" }
        AlertDialog(
            onDismissRequest = { pendingAccessibilityAction = null },
            containerColor = Color(0xFF101B2B),
            title = { Text("ПОДТВЕРДИ ДЕЙСТВИЕ", color = Color(0xFF00E5FF), fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Подготовить нажатие «$target» в приложении «$targetApp»? После подтверждения открой именно это приложение и коснись острова JARVIS. Он нажмёт только элемент с этой подписью в указанном приложении. Запрос действует 60 секунд.",
                    color = Color(0xFFE4F0F7), fontSize = 13.sp
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    AccessibilityBridge.queueClick(target, targetPackage, targetApp)
                    val reply = "Действие «$target» в приложении $targetApp подготовлено. Открой $targetApp и нажми на остров JARVIS для выполнения."
                    messages = messages + ChatMessage(reply, false)
                    voice.speak(reply)
                    pendingAccessibilityAction = null
                }) { Text("ПОДТВЕРДИТЬ", color = Color(0xFF00E5FF)) }
            },
            dismissButton = {
                TextButton(onClick = { pendingAccessibilityAction = null }) { Text("ОТМЕНА", color = Color(0xFFFF8299)) }
            }
        )
    }
}

@Composable
private fun AppCatalogScreen(onClose: () -> Unit, initialFavoritesOnly: Boolean = false) {
    val context = LocalContext.current
    val catalog = remember { AppCatalog(context) }
    var apps by remember { mutableStateOf(catalog.loadLaunchableApps()) }
    var query by rememberSaveable { mutableStateOf("") }
    var showFavoritesOnly by rememberSaveable { mutableStateOf(initialFavoritesOnly) }
    var favoritePackages by remember { mutableStateOf(AppFavoritesStore.load(context)) }
    var message by remember { mutableStateOf("Выбери приложение или произнеси его название.") }

    val filteredApps = remember(apps, query, showFavoritesOnly, favoritePackages) {
        apps.filter { app ->
            (!showFavoritesOnly || app.packageName in favoritePackages) &&
                (query.isBlank() || app.label.contains(query, ignoreCase = true) || app.packageName.contains(query, ignoreCase = true))
        }
    }

    LegionBackdrop {
        Column(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("APP CONTROL", color = LegionPalette.white, fontSize = 21.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                    Text("DEDSEC // INSTALLED APPLICATIONS", color = LegionPalette.cyan, fontSize = 8.sp, letterSpacing = 1.sp)
                }
                TextButton(onClick = onClose) { Text("← НАЗАД", color = LegionPalette.red, fontWeight = FontWeight.Black) }
            }
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Поиск приложения…", color = Color(0xFF71899B)) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFF00E5FF),
                    unfocusedBorderColor = Color(0xFF24465A),
                    cursorColor = Color(0xFF00E5FF)
                )
            )
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                TextButton(onClick = { showFavoritesOnly = false }) { Text("ВСЕ", color = if (!showFavoritesOnly) Color(0xFF00E5FF) else Color(0xFF7895A8), fontSize = 10.sp) }
                TextButton(onClick = { showFavoritesOnly = true }) { Text("★ ИЗБРАННОЕ", color = if (showFavoritesOnly) LegionPalette.amber else Color(0xFF7895A8), fontSize = 10.sp) }
                Spacer(Modifier.weight(1f))
                Text("${filteredApps.size} / ${apps.size}", color = Color(0xFF8DA7B9), fontSize = 10.sp)
                TextButton(onClick = { apps = catalog.loadLaunchableApps(); favoritePackages = AppFavoritesStore.load(context); message = "Список приложений обновлён." }) {
                    Text("↻", color = LegionPalette.cyan, fontSize = 16.sp)
                }
            }
            Card(
                modifier = Modifier.fillMaxWidth().weight(1f),
                colors = CardDefaults.cardColors(containerColor = LegionPalette.panel),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF245064))
            ) {
                if (filteredApps.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(if (showFavoritesOnly) "Избранное пока пусто. Нажми ☆ рядом с приложением." else "Приложения не найдены", color = Color(0xFF8DA7B9), fontSize = 13.sp, textAlign = TextAlign.Center)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredApps, key = { it.packageName }) { app ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = LegionPalette.panelRaised),
                                shape = RoundedCornerShape(13.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF284D61))
                            ) {
                                Column(Modifier.fillMaxWidth().padding(10.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        val iconBitmap = remember(app.packageName) { app.icon.toBitmap(96, 96).asImageBitmap() }
                                        Image(bitmap = iconBitmap, contentDescription = app.label, modifier = Modifier.size(42.dp).clip(RoundedCornerShape(10.dp)))
                                        Spacer(Modifier.width(10.dp))
                                        Column(Modifier.weight(1f)) {
                                            Text(app.label, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            Text(app.packageName, color = Color(0xFF7895A8), fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        }
                                        TextButton(
                                            onClick = { favoritePackages = AppFavoritesStore.toggle(context, app.packageName); message = if (app.packageName in favoritePackages) "${app.label} добавлено в избранное." else "${app.label} удалено из избранного." },
                                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                                        ) { Text(if (app.packageName in favoritePackages) "★" else "☆", color = if (app.packageName in favoritePackages) LegionPalette.amber else Color(0xFF7895A8), fontSize = 22.sp) }
                                    }
                                    Spacer(Modifier.height(6.dp))
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Button(
                                            onClick = {
                                                message = if (catalog.launch(app)) "Запускаю ${app.label}." else "Не удалось открыть ${app.label}."
                                                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                                            },
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(10.dp),
                                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp, vertical = 4.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9B233A))
                                        ) { Text("ОТКРЫТЬ", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold) }
                                        TextButton(
                                            onClick = {
                                                message = if (catalog.openAppDetails(app)) "Открыты настройки ${app.label}." else "Не удалось открыть настройки."
                                                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                                            },
                                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 5.dp, vertical = 4.dp)
                                        ) { Text("ИНФО", color = LegionPalette.cyan, fontSize = 9.sp) }
                                        TextButton(
                                            onClick = {
                                                message = if (catalog.requestUninstall(app)) "Ожидается подтверждение Android для удаления ${app.label}." else "Не удалось открыть удаление."
                                                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                                            },
                                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 5.dp, vertical = 4.dp)
                                        ) { Text("УДАЛИТЬ", color = Color(0xFFFF6B8A), fontSize = 9.sp) }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(message, color = Color(0xFF8DA7B9), fontSize = 10.sp, maxLines = 2)
            Text("Команды: «покажи избранные приложения», «добавь Telegram в избранное»", color = Color(0xFF506A7D), fontSize = 8.sp, maxLines = 2)
            Spacer(Modifier.height(4.dp))
            Text("УДАЛЕНИЕ ИЗМЕНЯЕТСЯ ТОЛЬКО ПОСЛЕ СИСТЕМНОГО ПОДТВЕРЖДЕНИЯ ANDROID", color = Color(0xFF506A7D), fontSize = 7.sp, letterSpacing = 0.5.sp)
        }
    }
}

@Composable
private fun ModeChip(label: String, selected: Boolean, accent: Color, onClick: () -> Unit) {
    TextButton(onClick = onClick, shape = RoundedCornerShape(50), colors = ButtonDefaults.textButtonColors(containerColor = if (selected) accent.copy(alpha = 0.18f) else Color.Transparent)) {
        Text(label, color = if (selected) accent else Color(0xFF71899B), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
    }
}

@Composable
private fun MessageBubble(message: ChatMessage) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (message.fromUser) Arrangement.End else Arrangement.Start) {
        Column(
            modifier = Modifier.fillMaxWidth(0.88f)
                .clip(RoundedCornerShape(14.dp))
                .background(if (message.fromUser) Color(0xFF102A39) else Color(0xFF171521))
                .border(1.dp, if (message.fromUser) LegionPalette.cyan.copy(alpha = 0.55f) else LegionPalette.red.copy(alpha = 0.42f), RoundedCornerShape(14.dp))
                .padding(10.dp)
        ) {
            Text(if (message.fromUser) "YOU // COMMAND" else "JARVIS // RESPONSE", color = if (message.fromUser) LegionPalette.cyan else LegionPalette.red, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
            Spacer(Modifier.height(3.dp))
            Text(message.text, color = Color(0xFFE4F0F7), fontSize = 13.sp, lineHeight = 18.sp)
        }
    }
}

@Composable
private fun ActionButton(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, accent: Color, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(50.dp)) {
        Button(
            onClick = onClick,
            modifier = Modifier.size(42.dp),
            shape = RoundedCornerShape(15.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
            colors = ButtonDefaults.buttonColors(containerColor = accent.copy(alpha = 0.16f)),
            border = androidx.compose.foundation.BorderStroke(1.dp, accent.copy(alpha = 0.65f))
        ) { Icon(icon, contentDescription = label, tint = accent, modifier = Modifier.size(21.dp)) }
        Spacer(Modifier.height(4.dp))
        Text(label, color = Color(0xFF8DA7B9), fontSize = 7.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.3.sp, maxLines = 1)
    }
}

@Composable
internal fun JarvisCore(isListening: Boolean, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "jarvis-core")
    val pulse by transition.animateFloat(initialValue = 0.94f, targetValue = 1.06f, animationSpec = infiniteRepeatable(tween(1800, easing = LinearEasing), RepeatMode.Reverse), label = "pulse")
    val rotation by transition.animateFloat(initialValue = 0f, targetValue = 360f, animationSpec = infiniteRepeatable(tween(9000, easing = LinearEasing)), label = "rotation")
    val cyan = if (isListening) LegionPalette.red else LegionPalette.cyan
    Box(modifier, contentAlignment = Alignment.Center) {
      Canvas(Modifier.fillMaxSize()) {
        val c = Offset(size.width / 2f, size.height / 2f)
        val base = size.minDimension * 0.43f
        drawCircle(Brush.radialGradient(listOf(cyan.copy(alpha = 0.25f), Color(0xFF123B58).copy(alpha = 0.10f), Color.Transparent), c, base * 1.4f), base * 1.4f, c)
        drawCircle(cyan.copy(alpha = 0.14f), base * pulse, c)
        drawCircle(cyan.copy(alpha = 0.8f), base * 0.83f, c, style = Stroke(width = 2.dp.toPx()))
        drawCircle(Color(0xFF315D78), base * 0.68f, c, style = Stroke(width = 1.dp.toPx()))
        drawArc(cyan, rotation, 105f, false, topLeft = Offset(c.x - base, c.y - base), size = Size(base * 2, base * 2), style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round))
        drawArc(LegionPalette.red, rotation + 180f, 70f, false, topLeft = Offset(c.x - base * 0.9f, c.y - base * 0.9f), size = Size(base * 1.8f, base * 1.8f), style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))
        for (i in 0 until 36) {
            val angle = Math.toRadians((i * 10.0) + rotation)
            val inner = base * 1.02f
            val outer = if (i % 3 == 0) base * 1.13f else base * 1.07f
            val x1 = c.x + kotlin.math.cos(angle).toFloat() * inner
            val y1 = c.y + kotlin.math.sin(angle).toFloat() * inner
            val x2 = c.x + kotlin.math.cos(angle).toFloat() * outer
            val y2 = c.y + kotlin.math.sin(angle).toFloat() * outer
            drawLine(cyan.copy(alpha = if (i % 3 == 0) 0.9f else 0.35f), Offset(x1, y1), Offset(x2, y2), strokeWidth = if (i % 3 == 0) 2.dp.toPx() else 1.dp.toPx())
        }
        drawCircle(Color(0xFF06101B), base * 0.56f, c)
        drawCircle(cyan.copy(alpha = 0.85f), base * 0.48f, c, style = Stroke(width = 1.5.dp.toPx()))
      }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("J", color = Color.White, fontSize = 58.sp, fontWeight = FontWeight.ExtraLight, letterSpacing = 3.sp)
            Text("AI CORE", color = cyan, fontSize = 8.sp, letterSpacing = 2.sp)
        }
    }
}

@Composable
private fun CyberGridBackground() {
    Canvas(Modifier.fillMaxSize()) {
        val gridColor = Color(0xFF17425A).copy(alpha = 0.18f)
        val step = 42.dp.toPx()
        var x = 0f
        while (x < size.width) {
            drawLine(gridColor, Offset(x, 0f), Offset(x, size.height), 1f)
            x += step
        }
        var y = 0f
        while (y < size.height) {
            drawLine(gridColor, Offset(0f, y), Offset(size.width, y), 1f)
            y += step
        }
        drawCircle(Color(0xFF00D9F5).copy(alpha = 0.05f), size.width * 0.48f, Offset(size.width * 0.5f, size.height * 0.31f))
    }
}
