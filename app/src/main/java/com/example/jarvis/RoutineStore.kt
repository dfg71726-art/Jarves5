package com.example.jarvis

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

 data class JarvisRoutine(val id: Long, val name: String, val commands: List<String>)

object RoutineStore {
    private const val PREFS = "jarvis_routines"
    private const val KEY = "saved_routines"

    fun load(context: Context): List<JarvisRoutine> {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, "[]") ?: "[]"
        return try {
            val array = JSONArray(raw)
            (0 until array.length()).mapNotNull { index ->
                val item = array.optJSONObject(index) ?: return@mapNotNull null
                val commandsJson = item.optJSONArray("commands") ?: JSONArray()
                val commands = (0 until commandsJson.length()).mapNotNull { commandsJson.optString(it).takeIf { it.isNotBlank() } }
                val name = item.optString("name").trim()
                if (name.isBlank() || commands.isEmpty()) null else JarvisRoutine(item.optLong("id"), name, commands)
            }
        } catch (_: Exception) { emptyList() }
    }

    fun save(context: Context, routines: List<JarvisRoutine>) {
        val array = JSONArray()
        routines.forEach { routine ->
            val commands = JSONArray()
            routine.commands.forEach { commands.put(it) }
            array.put(JSONObject().put("id", routine.id).put("name", routine.name).put("commands", commands))
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, array.toString()).apply()
    }

    /** Only simple app-launch and media transport commands are allowed in one-tap routines. */
    fun isAllowedCommand(raw: String): Boolean {
        val command = raw.trim().lowercase(Locale.forLanguageTag("ru-RU"))
        if (command.length !in 3..100) return false
        val launchesApp = (command.startsWith("открой ") || command.startsWith("запусти ")) &&
            command.substringAfter(' ').isNotBlank()
        return launchesApp || command in setOf("пауза", "останови музыку", "следующий трек", "следующая песня", "предыдущий трек", "предыдущая песня", "продолжи музыку", "включи воспроизведение")
    }
}
