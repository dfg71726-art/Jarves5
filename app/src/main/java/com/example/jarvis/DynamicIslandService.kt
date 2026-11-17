package com.example.jarvis

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.animation.ValueAnimator
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import kotlin.math.abs

/** User-enabled floating pill. The overlay itself never accesses the microphone or camera. */
class DynamicIslandService : Service() {
    private var windowManager: WindowManager? = null
    private var islandView: TextView? = null
    private var layoutParams: WindowManager.LayoutParams? = null
    private var expanded = false
    private var currentStatus = "СИСТЕМА ГОТОВА"
    private var waveformAnimator: ValueAnimator? = null
    private var waveformFrame = 0

    override fun onCreate() {
        super.onCreate()
        running = true
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_UPDATE -> {
                currentStatus = intent.getStringExtra(EXTRA_STATUS) ?: currentStatus
                renderText()
                updateWaveform()
                return START_STICKY
            }
            else -> {
                currentStatus = intent?.getStringExtra(EXTRA_STATUS) ?: currentStatus
                startAsForeground()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !android.provider.Settings.canDrawOverlays(this)) {
                    stopSelf()
                    return START_NOT_STICKY
                }
                showIsland()
            }
        }
        return START_STICKY
    }

    private fun startAsForeground() {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pending = PendingIntent.getActivity(
            this, 0, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("JARVIS // LEGION LINK")
            .setContentText("Плавающий индикатор включён. Нажми, чтобы открыть JARVIS.")
            .setContentIntent(pending)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun showIsland() {
        if (islandView != null) return
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        val density = resources.displayMetrics.density
        val pill = TextView(this).apply {
            textSize = 10.5f
            setTextColor(android.graphics.Color.rgb(210, 244, 255))
            typeface = Typeface.create("sans-serif-condensed", Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding((17 * density).toInt(), (9 * density).toInt(), (17 * density).toInt(), (9 * density).toInt())
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 60 * density
                setColor(android.graphics.Color.rgb(5, 8, 14))
                setStroke((1.6f * density).toInt(), android.graphics.Color.rgb(255, 54, 88))
            }
            elevation = 12 * density
            contentDescription = "JARVIS Legion Link. Нажми, чтобы открыть помощника."
            setOnClickListener { handleIslandTap() }
        }
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = (8 * density).toInt()
        }
        pill.setOnTouchListener(object : View.OnTouchListener {
            private var downX = 0f
            private var downY = 0f
            private var startX = 0
            private var startY = 0
            private var moved = false
            private var downTime = 0L
            private val threshold = 8 * density

            override fun onTouch(view: View, event: MotionEvent): Boolean {
                val lp = layoutParams ?: return false
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        downX = event.rawX
                        downY = event.rawY
                        downTime = event.eventTime
                        startX = lp.x
                        startY = lp.y
                        moved = false
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = event.rawX - downX
                        val dy = event.rawY - downY
                        if (abs(dx) > threshold || abs(dy) > threshold) moved = true
                        if (moved) {
                            lp.x = startX + dx.toInt()
                            lp.y = (startY + dy).toInt().coerceAtLeast(0)
                            try { windowManager?.updateViewLayout(view, lp) } catch (_: Exception) { }
                        }
                        return true
                    }
                    MotionEvent.ACTION_UP -> {
                        if (!moved) {
                            if (event.eventTime - downTime >= 500L) {
                                expanded = !expanded
                                renderText()
                            } else {
                                view.performClick()
                            }
                        }
                        return true
                    }
                    MotionEvent.ACTION_CANCEL -> return true
                }
                return false
            }
        })
        islandView = pill
        layoutParams = params
        renderText()
        updateWaveform()
        try {
            windowManager?.addView(pill, params)
        } catch (_: Exception) {
            islandView = null
            stopSelf()
        }
    }

    private fun renderText() {
        val view = islandView ?: return
        val active = isVoiceActive()
        val wave = if (active) waveFrames[waveformFrame.coerceIn(0, waveFrames.lastIndex)] else ""
        view.text = if (expanded) {
            "◉ JARVIS  $wave\n$currentStatus  •  Нажми для голосовой команды"
        } else {
            "◉  JARVIS  $wave  •  ${shortStatus(currentStatus)}"
        }
    }

    private fun isVoiceActive(): Boolean = currentStatus.contains("СЛУШАЮ") ||
        currentStatus.contains("РАСПОЗНАЮ") || currentStatus.contains("ОТВЕЧАЮ")

    private fun updateWaveform() {
        if (isVoiceActive()) {
            if (waveformAnimator?.isRunning != true) {
                waveformAnimator = ValueAnimator.ofInt(0, waveFrames.lastIndex).apply {
                    duration = 760L
                    repeatCount = ValueAnimator.INFINITE
                    repeatMode = ValueAnimator.REVERSE
                    addUpdateListener {
                        waveformFrame = it.animatedValue as Int
                        renderText()
                    }
                    start()
                }
            }
        } else {
            waveformAnimator?.cancel()
            waveformAnimator = null
            waveformFrame = 0
            renderText()
        }
    }

    private fun shortStatus(value: String): String = value
        .replace("СИСТЕМА ", "")
        .replace("АНАЛИЗ ЗАПРОСА", "ДУМАЮ")
        .replace("АКТИВАЦИЯ JARVIS", "СЛУШАЮ")
        .take(22)

    private fun handleIslandTap() {
        val action = AccessibilityBridge.performPendingClick()
        if (action != null) {
            val message = when (action.outcome) {
                AccessibilityClickResult.Outcome.SUCCESS -> {
                    currentStatus = "НАЖАТО: ${action.label}"
                    "JARVIS нажал «${action.label}» в ${action.appLabel}"
                }
                AccessibilityClickResult.Outcome.WRONG_APP -> {
                    currentStatus = "ОТКРОЙ: ${action.appLabel}"
                    "Сначала открой приложение ${action.appLabel}, затем снова нажми остров JARVIS"
                }
                AccessibilityClickResult.Outcome.EXPIRED -> {
                    currentStatus = "КОМАНДА ИСТЕКЛА"
                    "Время действия команды истекло. Повтори запрос в JARVIS."
                }
                AccessibilityClickResult.Outcome.NOT_FOUND -> {
                    currentStatus = "НЕ НАЙДЕНО: ${action.label}"
                    "Не удалось найти видимый элемент «${action.label}» в ${action.appLabel}"
                }
                AccessibilityClickResult.Outcome.SERVICE_UNAVAILABLE -> {
                    currentStatus = "ACCESSIBILITY OFF"
                    "Служба специальных возможностей JARVIS не подключена. Проверь её в настройках Android."
                }
            }
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
            renderText()
            return
        }
        openJarvis(startListening = true)
    }

    private fun openJarvis(startListening: Boolean) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(MainActivity.EXTRA_START_LISTENING, startListening)
        }
        startActivity(intent)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "JARVIS floating island", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        islandView?.let { view ->
            try { windowManager?.removeView(view) } catch (_: Exception) { }
        }
        islandView = null
        waveformAnimator?.cancel()
        waveformAnimator = null
        running = false
        stopForeground(STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val CHANNEL_ID = "jarvis_dynamic_island"
        private const val NOTIFICATION_ID = 7310
        private const val ACTION_START = "com.example.jarvis.action.ISLAND_START"
        private const val ACTION_UPDATE = "com.example.jarvis.action.ISLAND_UPDATE"
        private const val ACTION_STOP = "com.example.jarvis.action.ISLAND_STOP"
        private const val EXTRA_STATUS = "status"
        private val waveFrames = listOf("▁▃▅▇▅▃", "▃▅▇▅▃▁", "▅▇▅▃▁▃", "▇▅▃▁▃▅")
        @Volatile private var running = false

        fun start(context: Context, status: String = "СИСТЕМА ГОТОВА") {
            val intent = Intent(context, DynamicIslandService::class.java)
                .setAction(ACTION_START)
                .putExtra(EXTRA_STATUS, status)
            ContextCompat.startForegroundService(context, intent)
        }

        fun update(context: Context, status: String) {
            if (!running) return
            val intent = Intent(context, DynamicIslandService::class.java)
                .setAction(ACTION_UPDATE)
                .putExtra(EXTRA_STATUS, status)
            try { context.startService(intent) } catch (_: Exception) { }
        }

        fun stop(context: Context) {
            context.startService(Intent(context, DynamicIslandService::class.java).setAction(ACTION_STOP))
        }
    }
}
