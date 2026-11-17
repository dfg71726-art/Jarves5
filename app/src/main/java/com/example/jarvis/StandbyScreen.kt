package com.example.jarvis

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlaylistAddCheck
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** In-app standby dashboard. Android's secure lock screen remains system-controlled. */
@Composable
fun StandbyScreen(
    onClose: () -> Unit,
    onVoice: () -> Unit,
    onApps: () -> Unit,
    onNotifications: () -> Unit,
    onRoutines: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val catalog = remember { AppCatalog(context) }
    var now by remember { mutableStateOf(Date()) }
    var favorites by remember { mutableStateOf(emptyList<InstalledApp>()) }

    LaunchedEffect(Unit) {
        val favoritePackages = AppFavoritesStore.load(context)
        favorites = catalog.loadLaunchableApps().filter { it.packageName in favoritePackages }.take(8)
        while (true) {
            now = Date()
            delay(1000L)
        }
    }

    val time = remember(now) { SimpleDateFormat("HH:mm", Locale.getDefault()).format(now) }
    val date = remember(now) { SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(now).uppercase(Locale.getDefault()) }

    LegionBackdrop {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 17.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("JARVIS", color = LegionPalette.white, fontSize = 23.sp, fontWeight = FontWeight.Black, letterSpacing = 3.sp)
                    Text("DEDSEC // STANDBY NODE", color = LegionPalette.muted, fontSize = 8.sp, letterSpacing = 1.4.sp)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("● SYSTEM ONLINE", color = LegionPalette.green, fontSize = 9.sp, fontWeight = FontWeight.Black)
                    TextButton(onClick = onClose) { Text("ЗАКРЫТЬ  ×", color = LegionPalette.red, fontSize = 9.sp, fontWeight = FontWeight.Bold) }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(topStart = 5.dp, topEnd = 18.dp, bottomEnd = 5.dp, bottomStart = 18.dp),
                colors = CardDefaults.cardColors(containerColor = LegionPalette.panel),
                border = androidx.compose.foundation.BorderStroke(1.dp, LegionPalette.cyan.copy(alpha = 0.4f))
            ) {
                Column(Modifier.fillMaxWidth().padding(vertical = 11.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(time, color = LegionPalette.white, fontSize = 58.sp, fontWeight = FontWeight.ExtraLight, letterSpacing = 3.sp)
                    Text(date, color = LegionPalette.cyan, fontSize = 9.sp, letterSpacing = 1.8.sp, textAlign = TextAlign.Center)
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(3.dp)) {
                JarvisCore(isListening = false, modifier = Modifier.size(224.dp))
                Text("NEURAL CORE // READY", color = LegionPalette.green, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.7.sp)
                Text("ОЖИДАНИЕ ГОЛОСОВОЙ КОМАНДЫ", color = LegionPalette.muted, fontSize = 8.sp, letterSpacing = 1.sp)
            }

            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LegionSectionLabel("FAVORITE APPS", "${favorites.size} PINNED")
                if (favorites.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        items(favorites, key = { it.packageName }) { app ->
                            TextButton(
                                onClick = { catalog.launch(app) },
                                modifier = Modifier.border(1.dp, LegionPalette.cyan.copy(alpha = 0.45f), RoundedCornerShape(8.dp)).background(LegionPalette.panelRaised, RoundedCornerShape(8.dp))
                            ) {
                                Text(app.label, color = LegionPalette.white, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                } else {
                    Text("Добавь приложения в избранное через APP CONTROL", color = LegionPalette.muted, fontSize = 9.sp)
                }

                LegionSectionLabel("QUICK COMMANDS")
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    LegionActionTile("ГОЛОС", "START LISTENING", Icons.Default.Mic, LegionPalette.red, Modifier.weight(1f), onVoice)
                    LegionActionTile("ПРИЛОЖЕНИЯ", "APP CONTROL", Icons.Default.Apps, LegionPalette.cyan, Modifier.weight(1f), onApps)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    LegionActionTile("УВЕДОМЛЕНИЯ", "MESSAGE HUB", Icons.Default.Notifications, LegionPalette.cyan, Modifier.weight(1f), onNotifications)
                    LegionActionTile("СЦЕНАРИИ", "AUTOMATION", Icons.Default.PlaylistAddCheck, LegionPalette.red, Modifier.weight(1f), onRoutines)
                }
                Text(
                    "STANDBY — ВНУТРЕННИЙ ЭКРАН JARVIS. СИСТЕМНАЯ БЛОКИРОВКА, PIN И БИОМЕТРИЯ ОСТАЮТСЯ ПОД КОНТРОЛЕМ ANDROID.",
                    color = Color(0xFF536D80), fontSize = 7.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 2.dp)
                )
            }
        }
    }
}
