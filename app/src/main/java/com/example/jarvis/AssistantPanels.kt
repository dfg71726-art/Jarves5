package com.example.jarvis

import android.content.Context
import android.widget.Toast
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val panelCyan = LegionPalette.cyan
private val panelMuted = LegionPalette.muted
private val panelBg = LegionPalette.panelRaised

@Composable
fun NotificationCenterScreen(onClose: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val notifications by NotificationBridge.notifications.collectAsState()
    var selected by remember { mutableStateOf<JarvisNotification?>(null) }
    var replyText by rememberSaveable { mutableStateOf("") }
    var permissionRefresh by remember { mutableStateOf(false) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) permissionRefresh = !permissionRefresh
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val hasAccess = remember(permissionRefresh) { NotificationBridge.hasAccess(context) }

    PanelBackground {
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            PanelHeader("NOTIFICATION HUB", "ЦЕНТР УВЕДОМЛЕНИЙ JARVIS", onClose)
            Spacer(Modifier.height(12.dp))
            if (!hasAccess) {
                Card(colors = CardDefaults.cardColors(containerColor = panelBg), shape = RoundedCornerShape(16.dp)) {
                    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("НУЖНО РАЗРЕШЕНИЕ", color = panelCyan, fontWeight = FontWeight.Bold)
                        Text("Чтобы видеть уведомления и отвечать на поддерживаемые сообщения, разреши доступ JARVIS в системных настройках Android. Содержимое останется на устройстве.", color = panelMuted, fontSize = 12.sp)
                        Button(onClick = { NotificationBridge.openAccessSettings(context); permissionRefresh = !permissionRefresh }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF07566B))) {
                            Text("ОТКРЫТЬ НАСТРОЙКИ ДОСТУПА")
                        }
                    }
                }
            } else {
                Text("АКТИВНЫХ УВЕДОМЛЕНИЙ: ${notifications.size}", color = panelCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Spacer(Modifier.height(8.dp))
                if (notifications.isEmpty()) {
                    Text("Пока нет уведомлений. Новые появятся здесь после их поступления.", color = panelMuted, fontSize = 13.sp)
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(9.dp), modifier = Modifier.weight(1f)) {
                        items(notifications, key = { it.key }) { item ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = panelBg),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth().border(1.dp, Color(0xFF1B4358), RoundedCornerShape(14.dp))
                            ) {
                                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Notifications, contentDescription = null, tint = panelCyan)
                                        Spacer(Modifier.width(7.dp))
                                        Text(item.appLabel, color = panelCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                        Text(SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(item.postedAt)), color = panelMuted, fontSize = 9.sp)
                                    }
                                    if (item.title.isNotBlank()) Text(item.title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                    if (item.text.isNotBlank()) Text(item.text, color = panelMuted, fontSize = 12.sp, maxLines = 4)
                                    if (item.canReply) {
                                        TextButton(onClick = { selected = item; replyText = "" }) {
                                            Icon(Icons.Default.Send, contentDescription = null, tint = panelCyan)
                                            Spacer(Modifier.width(5.dp))
                                            Text("ОТВЕТИТЬ", color = panelCyan, fontSize = 10.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Text("JARVIS не отправляет ответы автоматически. Каждая отправка требует отдельного подтверждения.", color = Color(0xFF70889A), fontSize = 9.sp)
        }
    }

    selected?.let { item ->
        AlertDialog(
            onDismissRequest = { selected = null },
            containerColor = Color(0xFF101B2B),
            title = { Text("ОТВЕТ В ${item.appLabel.uppercase(Locale.getDefault())}", color = panelCyan, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(item.title.ifBlank { item.text }.take(180), color = Color.White, fontSize = 12.sp)
                    OutlinedTextField(
                        value = replyText,
                        onValueChange = { replyText = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Напиши ответ…", color = Color.Gray) },
                        minLines = 2,
                        maxLines = 4,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = panelCyan, unfocusedBorderColor = Color(0xFF345064))
                    )
                    Text("Проверь текст. После подтверждения он будет отправлен через действие ответа исходного уведомления.", color = panelMuted, fontSize = 10.sp)
                }
            },
            confirmButton = {
                TextButton(enabled = replyText.isNotBlank(), onClick = {
                    val sent = NotificationBridge.sendReply(context, item.key, replyText.trim())
                    Toast.makeText(context, if (sent) "Ответ передан приложению" else "Не удалось отправить ответ. Возможно, уведомление уже закрыто.", Toast.LENGTH_LONG).show()
                    selected = null
                }) { Text("ПОДТВЕРДИТЬ И ОТПРАВИТЬ", color = panelCyan) }
            },
            dismissButton = { TextButton(onClick = { selected = null }) { Text("ОТМЕНА", color = Color(0xFFFF8299)) } }
        )
    }
}

@Composable
fun RoutinesScreen(onClose: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val routines = remember { mutableStateListOf<JarvisRoutine>().apply { addAll(RoutineStore.load(context)) } }
    var routineName by rememberSaveable { mutableStateOf("") }
    var commandInput by rememberSaveable { mutableStateOf("") }
    val draftCommands = remember { mutableStateListOf<String>() }
    var selectedRoutine by remember { mutableStateOf<JarvisRoutine?>(null) }
    var validationMessage by remember { mutableStateOf("Добавляй только запуск приложений и команды управления музыкой.") }
    val scope = rememberCoroutineScope()
    val router = remember {
        LocalCommandRouter(context, onShowApps = { _ -> }, onAccessibilityActionRequest = { _, _, _ -> })
    }

    PanelBackground {
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            PanelHeader("JARVIS ROUTINES", "ПЕРСОНАЛЬНЫЕ СЦЕНАРИИ", onClose)
            Spacer(Modifier.height(10.dp))
            Card(colors = CardDefaults.cardColors(containerColor = panelBg), shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("НОВЫЙ СЦЕНАРИЙ", color = panelCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = routineName,
                        onValueChange = { routineName = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        placeholder = { Text("Название, например: Вечер", color = Color.Gray) },
                        colors = routineFieldColors()
                    )
                    OutlinedTextField(
                        value = commandInput,
                        onValueChange = { commandInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        placeholder = { Text("Команда, например: открой YouTube", color = Color.Gray) },
                        colors = routineFieldColors()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = {
                            if (RoutineStore.isAllowedCommand(commandInput)) {
                                draftCommands.add(commandInput.trim())
                                commandInput = ""
                                validationMessage = "Шаг добавлен."
                            } else validationMessage = "Команда не поддерживается. Используй «открой …», «запусти …», «пауза», «следующий трек» или «предыдущий трек»."
                        }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF07566B))) { Text("ДОБАВИТЬ ШАГ") }
                        Button(onClick = {
                            if (routineName.isNotBlank() && draftCommands.isNotEmpty()) {
                                val item = JarvisRoutine(System.currentTimeMillis(), routineName.trim(), draftCommands.toList())
                                routines.add(0, item)
                                RoutineStore.save(context, routines)
                                routineName = ""
                                draftCommands.clear()
                                validationMessage = "Сценарий сохранён."
                            } else validationMessage = "Укажи название и добавь хотя бы один шаг."
                        }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4B3475))) { Text("СОХРАНИТЬ") }
                    }
                    if (draftCommands.isNotEmpty()) {
                        Text("ШАГИ: ${draftCommands.joinToString("  →  ")}", color = Color.White, fontSize = 10.sp)
                        TextButton(onClick = { if (draftCommands.isNotEmpty()) draftCommands.removeAt(draftCommands.lastIndex) }) { Text("УБРАТЬ ПОСЛЕДНИЙ ШАГ", color = Color(0xFFFF8299), fontSize = 9.sp) }
                    }
                    Text(validationMessage, color = panelMuted, fontSize = 10.sp)
                }
            }
            Spacer(Modifier.height(12.dp))
            Text("СОХРАНЁННЫЕ СЦЕНАРИИ: ${routines.size}", color = panelCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            if (routines.isEmpty()) {
                Text("Пока нет сценариев. Создай первый выше.", color = panelMuted, fontSize = 12.sp)
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                    items(routines, key = { it.id }) { routine ->
                        Card(colors = CardDefaults.cardColors(containerColor = panelBg), shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(routine.name, color = Color.White, fontWeight = FontWeight.Bold)
                                Text(routine.commands.joinToString("  •  "), color = panelMuted, fontSize = 10.sp)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(onClick = { selectedRoutine = routine }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF07566B))) {
                                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                                        Text("ЗАПУСТИТЬ")
                                    }
                                    TextButton(onClick = {
                                        routines.removeAll { it.id == routine.id }
                                        RoutineStore.save(context, routines)
                                    }) {
                                        Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFFF8299))
                                        Text("УДАЛИТЬ", color = Color(0xFFFF8299))
                                    }
                                }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            Text("Сценарии выполняют только добавленные тобой команды запуска приложений и управления музыкой. Перед каждым запуском показывается подтверждение.", color = Color(0xFF70889A), fontSize = 9.sp)
        }
    }

    selectedRoutine?.let { routine ->
        AlertDialog(
            onDismissRequest = { selectedRoutine = null },
            containerColor = Color(0xFF101B2B),
            title = { Text("ЗАПУСТИТЬ СЦЕНАРИЙ?", color = panelCyan, fontWeight = FontWeight.Bold) },
            text = { Text("${routine.name}\n\n${routine.commands.mapIndexed { index, command -> "${index + 1}. $command" }.joinToString("\n")}\n\nJARVIS выполнит эти шаги по порядку.", color = Color.White, fontSize = 12.sp) },
            confirmButton = {
                TextButton(onClick = {
                    selectedRoutine = null
                    scope.launch {
                        routine.commands.forEach { command ->
                            router.execute(command)
                            delay(500)
                        }
                        Toast.makeText(context, "Сценарий «${routine.name}» выполнен", Toast.LENGTH_LONG).show()
                    }
                }) { Text("ПОДТВЕРДИТЬ ЗАПУСК", color = panelCyan) }
            },
            dismissButton = { TextButton(onClick = { selectedRoutine = null }) { Text("ОТМЕНА", color = Color(0xFFFF8299)) } }
        )
    }
}

@Composable
private fun PanelBackground(content: @Composable () -> Unit) {
    LegionBackdrop { content() }
}

@Composable
private fun PanelHeader(title: String, subtitle: String, onClose: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            Box(Modifier.width(4.dp).height(34.dp).background(LegionPalette.red, RoundedCornerShape(2.dp)))
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, color = LegionPalette.white, fontSize = 18.sp, fontWeight = FontWeight.Black, letterSpacing = 1.4.sp)
                Text(subtitle, color = panelCyan, fontSize = 8.sp, letterSpacing = 1.1.sp)
            }
        }
        TextButton(onClick = onClose) { Text("← НАЗАД", color = LegionPalette.red, fontWeight = FontWeight.Black, fontSize = 10.sp) }
    }
}

@Composable
private fun routineFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White,
    focusedBorderColor = panelCyan,
    unfocusedBorderColor = Color(0xFF345064),
    cursorColor = panelCyan
)
