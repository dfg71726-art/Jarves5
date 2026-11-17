package com.example.jarvis

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.net.Uri
import android.provider.Settings

/** A launchable app visible to JARVIS through Android's launcher query. */
data class InstalledApp(
    val label: String,
    val packageName: String,
    val icon: Drawable
)

class AppCatalog(private val context: Context) {
    private val packageManager: PackageManager
        get() = context.packageManager

    fun loadLaunchableApps(): List<InstalledApp> {
        val launcherQuery = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return packageManager.queryIntentActivities(launcherQuery, 0)
            .mapNotNull { resolved ->
                val packageName = resolved.activityInfo?.packageName ?: return@mapNotNull null
                try {
                    InstalledApp(
                        label = resolved.loadLabel(packageManager).toString(),
                        packageName = packageName,
                        icon = resolved.loadIcon(packageManager)
                    )
                } catch (_: Exception) {
                    null
                }
            }
            .filter { it.packageName != context.packageName }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }
    }

    fun findApp(query: String): InstalledApp? {
        val normalized = normalize(query)
        if (normalized.isBlank()) return null
        val apps = loadLaunchableApps()
        return apps.firstOrNull { normalize(it.label) == normalized }
            ?: apps.firstOrNull { normalize(it.label).contains(normalized) }
            ?: apps.firstOrNull { normalized.contains(normalize(it.label)) }
    }

    fun launch(app: InstalledApp): Boolean {
        val launchIntent = packageManager.getLaunchIntentForPackage(app.packageName) ?: return false
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
            context.startActivity(launchIntent)
            true
        } catch (_: Exception) {
            false
        }
    }

    fun openAppDetails(app: InstalledApp): Boolean {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            .setData(Uri.fromParts("package", app.packageName, null))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return startSafely(intent)
    }

    fun openNotificationSettings(app: InstalledApp): Boolean {
        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, app.packageName)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return startSafely(intent) || openAppDetails(app)
    }

    /** Android displays its own confirmation UI; JARVIS never silently uninstalls an app. */
    fun requestUninstall(app: InstalledApp): Boolean {
        val intent = Intent(Intent.ACTION_DELETE, Uri.parse("package:${app.packageName}"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return startSafely(intent)
    }

    private fun startSafely(intent: Intent): Boolean = try {
        context.startActivity(intent)
        true
    } catch (_: Exception) {
        false
    }

    private fun normalize(value: String): String = value
        .lowercase()
        .replace(Regex("[^\\p{L}\\p{N}]"), "")
}


/** User-managed app favorites stored locally on this device. */
object AppFavoritesStore {
    private const val PREFS = "jarvis_app_control"
    private const val KEY_FAVORITES = "favorite_packages"

    fun load(context: Context): Set<String> =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getStringSet(KEY_FAVORITES, emptySet())?.toSet() ?: emptySet()

    fun setFavorite(context: Context, packageName: String, favorite: Boolean): Boolean {
        val current = load(context).toMutableSet()
        if (favorite) current.add(packageName) else current.remove(packageName)
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putStringSet(KEY_FAVORITES, current.toSet()).apply()
        return favorite
    }

    fun toggle(context: Context, packageName: String): Boolean =
        setFavorite(context, packageName, packageName !in load(context))
}
