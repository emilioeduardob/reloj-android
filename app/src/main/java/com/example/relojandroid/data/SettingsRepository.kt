package com.example.relojandroid.data

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "reloj_settings")

private val TAG = "SettingsRepository"
private val DEFAULT_SETTINGS = Settings()

class SettingsRepository(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true }

    private object Keys {
        val ENABLED_FACES = stringPreferencesKey("enabled_faces")
        val ROTATION_SECONDS = intPreferencesKey("rotation_seconds")
        val WEATHER_CITY = stringPreferencesKey("weather_city")
        val WEATHER_LAT = stringPreferencesKey("weather_lat")
        val WEATHER_LON = stringPreferencesKey("weather_lon")
        val EXCHANGE_SOURCE = stringPreferencesKey("exchange_source")
        val SERVER_PORT = intPreferencesKey("server_port")
        val BRIGHTNESS = floatPreferencesKey("brightness")
        val CLOCK_ART = stringPreferencesKey("clock_art")
    }

    val settings: Flow<Settings> = context.dataStore.data.map { prefs ->
        Settings(
            enabledFaces = prefs.parseStringList(Keys.ENABLED_FACES) ?: DEFAULT_SETTINGS.enabledFaces,
            rotationSeconds = prefs[Keys.ROTATION_SECONDS] ?: DEFAULT_SETTINGS.rotationSeconds,
            weatherCity = prefs[Keys.WEATHER_CITY] ?: DEFAULT_SETTINGS.weatherCity,
            weatherLat = prefs[Keys.WEATHER_LAT]?.toDoubleOrNull() ?: DEFAULT_SETTINGS.weatherLat,
            weatherLon = prefs[Keys.WEATHER_LON]?.toDoubleOrNull() ?: DEFAULT_SETTINGS.weatherLon,
            exchangeSource = prefs[Keys.EXCHANGE_SOURCE] ?: DEFAULT_SETTINGS.exchangeSource,
            serverPort = prefs[Keys.SERVER_PORT] ?: DEFAULT_SETTINGS.serverPort,
            brightness = prefs[Keys.BRIGHTNESS] ?: DEFAULT_SETTINGS.brightness,
            clockArt = prefs[Keys.CLOCK_ART] ?: DEFAULT_SETTINGS.clockArt
        )
    }

    suspend fun updateSettings(settings: Settings) {
        context.dataStore.edit { prefs ->
            prefs[Keys.ENABLED_FACES] = json.encodeToString(settings.enabledFaces)
            prefs[Keys.ROTATION_SECONDS] = settings.rotationSeconds
            prefs[Keys.WEATHER_CITY] = settings.weatherCity
            prefs[Keys.WEATHER_LAT] = settings.weatherLat.toString()
            prefs[Keys.WEATHER_LON] = settings.weatherLon.toString()
            prefs[Keys.EXCHANGE_SOURCE] = settings.exchangeSource
            prefs[Keys.SERVER_PORT] = settings.serverPort
            prefs[Keys.BRIGHTNESS] = settings.brightness
            prefs[Keys.CLOCK_ART] = settings.clockArt
        }
    }

    private fun Preferences.parseStringList(key: Preferences.Key<String>): List<String>? {
        val raw = this[key] ?: return null
        return try {
            json.decodeFromString<List<String>>(raw)
        } catch (e: SerializationException) {
            Log.w(TAG, "Failed to parse list for key ${key.name}, falling back to default", e)
            null
        }
    }
}
