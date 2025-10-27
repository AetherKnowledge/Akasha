package com.rosuelo.chatbot

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable

enum class Tools {
    WEBSEARCH,
    CALCULATOR,
}

private val Context.dataStore by preferencesDataStore(name = "settings")

object Settings {
    var enabledTools by mutableStateOf(setOf<Tools>())

    private val ENABLED_TOOLS_KEY = stringSetPreferencesKey("enabled_tools")

    /**
     * Call this once at app startup (e.g. in your Application or MainActivity)
     */
    fun initialize(context: Context) {
        runBlocking {
            val prefs = context.dataStore.data.first()
            val saved = prefs[ENABLED_TOOLS_KEY]

            enabledTools = saved?.mapNotNull { runCatching { Tools.valueOf(it) }.getOrNull() }
                ?.toSet()
                ?: Tools.entries.toSet() // Default to all tools enabled
        }
    }

    /**
     * Save enabled tools to DataStore
     */
    suspend fun save(context: Context) {
        context.dataStore.edit { prefs ->
            prefs[ENABLED_TOOLS_KEY] = enabledTools.map { it.name }.toSet()
        }
    }

    /**
     * Toggle tool on/off and persist
     */
    suspend fun toggle(context: Context, tool: Tools, enabled: Boolean) {
        enabledTools = if (enabled) {
            enabledTools + tool
        } else {
            enabledTools - tool
        }
        save(context)
    }
}