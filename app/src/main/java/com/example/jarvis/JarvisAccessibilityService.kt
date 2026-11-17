package com.example.jarvis

import android.accessibilityservice.AccessibilityService
import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

/**
 * Optional, user-enabled accessibility bridge.
 * It does not monitor or upload screen content. It only searches for the exact
 * visible label supplied by the user after an in-app confirmation and clicks
 * that labelled control when Android exposes it as clickable.
 */
class JarvisAccessibilityService : AccessibilityService() {
    override fun onServiceConnected() {
        super.onServiceConnected()
        AccessibilityBridge.attach(this)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Intentionally empty: no event stream, screen scraping, or logging.
    }

    override fun onInterrupt() = Unit

    override fun onUnbind(intent: android.content.Intent?): Boolean {
        AccessibilityBridge.detach(this)
        return super.onUnbind(intent)
    }

    fun activeWindowPackage(): String? = rootInActiveWindow?.packageName?.toString()

    fun clickVisibleLabel(label: String, expectedPackage: String): Boolean {
        val root = rootInActiveWindow ?: return false
        if (root.packageName?.toString() != expectedPackage) return false
        val candidates = try {
            root.findAccessibilityNodeInfosByText(label)
        } catch (_: Exception) {
            emptyList()
        }
        for (candidate in candidates) {
            if (!candidate.isVisibleToUser) continue
            val visibleText = candidate.text?.toString()?.trim()
            val visibleDescription = candidate.contentDescription?.toString()?.trim()
            if (!visibleText.equals(label, ignoreCase = true) &&
                !visibleDescription.equals(label, ignoreCase = true)) continue
            var node: AccessibilityNodeInfo? = candidate
            var depth = 0
            while (node != null && depth < 5) {
                if (node.isVisibleToUser && node.isEnabled && node.isClickable) {
                    if (node.performAction(AccessibilityNodeInfo.ACTION_CLICK)) return true
                }
                node = node.parent
                depth++
            }
        }
        return false
    }
}

data class PendingAccessibilityClick(
    val label: String,
    val packageName: String,
    val appLabel: String,
    val queuedAtMillis: Long
)

data class AccessibilityClickResult(
    val label: String,
    val appLabel: String,
    val outcome: Outcome
) {
    enum class Outcome { SUCCESS, WRONG_APP, EXPIRED, NOT_FOUND, SERVICE_UNAVAILABLE }
}

object AccessibilityBridge {
    @Volatile private var activeService: JarvisAccessibilityService? = null
    @Volatile private var pendingClick: PendingAccessibilityClick? = null

    internal fun attach(service: JarvisAccessibilityService) {
        activeService = service
    }

    internal fun detach(service: JarvisAccessibilityService) {
        if (activeService === service) activeService = null
    }

    fun isEnabled(context: Context): Boolean {
        val expected = ComponentName(context, JarvisAccessibilityService::class.java).flattenToString()
        val enabled = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        return enabled.split(':').any { it.equals(expected, ignoreCase = true) }
    }

    fun queueClick(label: String, packageName: String, appLabel: String) {
        pendingClick = PendingAccessibilityClick(label, packageName, appLabel, System.currentTimeMillis())
    }

    fun cancelPendingClick() {
        pendingClick = null
    }

    fun performPendingClick(): AccessibilityClickResult? {
        val pending = pendingClick ?: return null
        if (System.currentTimeMillis() - pending.queuedAtMillis > 60_000L) {
            pendingClick = null
            return AccessibilityClickResult(pending.label, pending.appLabel, AccessibilityClickResult.Outcome.EXPIRED)
        }
        val service = activeService
            ?: return AccessibilityClickResult(pending.label, pending.appLabel, AccessibilityClickResult.Outcome.SERVICE_UNAVAILABLE)
        val activePackage = service.activeWindowPackage()
        if (activePackage != pending.packageName) {
            return AccessibilityClickResult(pending.label, pending.appLabel, AccessibilityClickResult.Outcome.WRONG_APP)
        }
        val clicked = service.clickVisibleLabel(pending.label, pending.packageName)
        if (clicked) {
            pendingClick = null
            return AccessibilityClickResult(pending.label, pending.appLabel, AccessibilityClickResult.Outcome.SUCCESS)
        }
        return AccessibilityClickResult(pending.label, pending.appLabel, AccessibilityClickResult.Outcome.NOT_FOUND)
    }
}
