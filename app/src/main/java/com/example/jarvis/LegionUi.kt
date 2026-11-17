package com.example.jarvis

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

internal object LegionPalette {
    val background = Color(0xFF05070D)
    val panel = Color(0xE6101722)
    val panelRaised = Color(0xF0141D29)
    val cyan = Color(0xFF32D9FF)
    val red = Color(0xFFFF3658)
    val muted = Color(0xFF8296A8)
    val white = Color(0xFFEAF5FC)
    val green = Color(0xFF48F0B5)
    val amber = Color(0xFFFFC857)
}


@Composable
internal fun LegionTheme(content: @Composable () -> Unit) {
    val colors = darkColorScheme(
        primary = LegionPalette.red,
        secondary = LegionPalette.cyan,
        tertiary = LegionPalette.green,
        background = LegionPalette.background,
        surface = LegionPalette.panelRaised,
        onPrimary = Color.White,
        onSecondary = LegionPalette.background,
        onBackground = LegionPalette.white,
        onSurface = LegionPalette.white
    )
    MaterialTheme(colorScheme = colors, content = content)
}

@Composable
internal fun LegionBackdrop(content: @Composable () -> Unit) {
    Box(
        Modifier.fillMaxSize().background(
            Brush.verticalGradient(
                listOf(Color(0xFF03050A), Color(0xFF0A111B), Color(0xFF070A12), Color(0xFF03050A))
            )
        )
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val grid = Color(0xFF1B536A).copy(alpha = 0.13f)
            val step = 34.dp.toPx()
            var x = 0f
            while (x < size.width) {
                drawLine(grid, androidx.compose.ui.geometry.Offset(x, 0f), androidx.compose.ui.geometry.Offset(x, size.height), 0.7.dp.toPx())
                x += step
            }
            var y = 0f
            while (y < size.height) {
                drawLine(grid, androidx.compose.ui.geometry.Offset(0f, y), androidx.compose.ui.geometry.Offset(size.width, y), 0.7.dp.toPx())
                y += step
            }
            for (i in 0..18) {
                val yy = i * (size.height / 18f)
                drawLine(Color.White.copy(alpha = 0.018f), androidx.compose.ui.geometry.Offset(0f, yy), androidx.compose.ui.geometry.Offset(size.width, yy), 1f)
            }
            val corner = 22.dp.toPx()
            val stroke = 2.dp.toPx()
            val red = LegionPalette.red.copy(alpha = 0.72f)
            val cyan = LegionPalette.cyan.copy(alpha = 0.72f)
            // Small HUD corner brackets.
            drawLine(cyan, androidx.compose.ui.geometry.Offset(12f, 26f), androidx.compose.ui.geometry.Offset(12f + corner, 26f), stroke, cap = StrokeCap.Square)
            drawLine(cyan, androidx.compose.ui.geometry.Offset(12f, 26f), androidx.compose.ui.geometry.Offset(12f, 26f + corner), stroke, cap = StrokeCap.Square)
            drawLine(red, androidx.compose.ui.geometry.Offset(size.width - 12f, size.height - 26f), androidx.compose.ui.geometry.Offset(size.width - 12f - corner, size.height - 26f), stroke, cap = StrokeCap.Square)
            drawLine(red, androidx.compose.ui.geometry.Offset(size.width - 12f, size.height - 26f), androidx.compose.ui.geometry.Offset(size.width - 12f, size.height - 26f - corner), stroke, cap = StrokeCap.Square)
            drawCircle(Color(0xFF00BFEA).copy(alpha = 0.045f), size.width * 0.42f, androidx.compose.ui.geometry.Offset(size.width * 0.52f, size.height * 0.32f))
        }
        content()
    }
}

@Composable
internal fun LegionActionTile(
    label: String,
    caption: String,
    icon: ImageVector,
    accent: Color = LegionPalette.cyan,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = modifier.height(68.dp),
        shape = RoundedCornerShape(topStart = 5.dp, topEnd = 15.dp, bottomEnd = 5.dp, bottomStart = 15.dp),
        colors = CardDefaults.cardColors(containerColor = LegionPalette.panelRaised),
        border = androidx.compose.foundation.BorderStroke(1.dp, accent.copy(alpha = 0.52f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            Modifier.fillMaxSize().padding(horizontal = 9.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                Modifier.size(34.dp)
                    .background(accent.copy(alpha = 0.11f), RoundedCornerShape(9.dp))
                    .border(1.dp, accent.copy(alpha = 0.42f), RoundedCornerShape(9.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = label, tint = accent, modifier = Modifier.size(19.dp))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                Text(label, color = LegionPalette.white, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 0.6.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(2.dp))
                Text(caption, color = LegionPalette.muted, fontSize = 7.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
internal fun LegionSectionLabel(text: String, trailing: String = "") {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(width = 3.dp, height = 13.dp).background(LegionPalette.red))
        Text(text, color = LegionPalette.cyan, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.2.sp, modifier = Modifier.padding(start = 7.dp).weight(1f))
        if (trailing.isNotBlank()) Text(trailing, color = LegionPalette.muted, fontSize = 8.sp, letterSpacing = 0.7.sp)
    }
}

@Composable
internal fun LegionHudMetric(label: String, value: String, accent: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(7.dp),
        colors = CardDefaults.cardColors(containerColor = LegionPalette.panel),
        border = androidx.compose.foundation.BorderStroke(1.dp, accent.copy(alpha = 0.35f))
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 9.dp, vertical = 7.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(label, color = LegionPalette.muted, fontSize = 7.sp, letterSpacing = 1.sp, maxLines = 1)
            Text(value, color = accent, fontSize = 11.sp, fontWeight = FontWeight.Black, maxLines = 1)
        }
    }
}
