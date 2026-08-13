package com.example.relojandroid.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "reloj_settings")

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
    }

    val settings: Flow<Settings> = context.dataStore.data.map { prefs ->
        Settings(
            enabledFaces = prefs[Keys.ENABLED_FACES]?.let {
                json.decodeFromString(it)
            } ?: Settings().enabledFaces,
            rotationSeconds = prefs[Keys.ROTATION_SECONDS] ?: Settings().rotationSeconds,
            weatherCity = prefs[Keys.WEATHER_CITY] ?: Settings().weatherCity,
            weatherLat = prefs[Keys.WEATHER_LAT]?.toDouble() ?: Settings().weatherLat,
            weatherLon = prefs[Keys.WEATHER_LON]?.toDouble() ?: Settings().weatherLon,
            exchangeSource = prefs[Keys.EXCHANGE_SOURCE] ?: Settings().exchangeSource,
            serverPort = prefs[Keys.SERVER_PORT] ?: Settings().serverPort,
            brightness = prefs[Keys.BRIGHTNESS] ?: Settings().brightness
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
        }
    }
}
