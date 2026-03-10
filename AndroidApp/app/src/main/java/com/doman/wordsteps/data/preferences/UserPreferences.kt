package com.doman.wordsteps.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_prefs")

class UserPreferences(private val context: Context) {

    companion object {
        val SERVER_IP = stringPreferencesKey("server_ip")
        const val DEFAULT_IP = "192.168.0.98:5000"
    }

    val serverIpFlow: Flow<String> = context.dataStore.data
        .map { prefs -> prefs[SERVER_IP] ?: DEFAULT_IP }

    suspend fun saveServerIp(ip: String) {
        context.dataStore.edit { prefs ->
            prefs[SERVER_IP] = ip
        }
    }
}