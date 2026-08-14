package com.example.relojandroid.data

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class WeatherApi {

    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true; isLenient = true })
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 15_000
            connectTimeoutMillis = 10_000
        }
    }

    suspend fun fetchCurrent(lat: Double, lon: Double): WeatherResult {
        val response: OpenMeteoResponse = client.get(
            "https://api.open-meteo.com/v1/forecast"
        ) {
            url {
                parameters.append("latitude", lat.toString())
                parameters.append("longitude", lon.toString())
                parameters.append("current_weather", "true")
                parameters.append("timezone", "auto")
            }
        }.body()

        return WeatherResult(
            temperature = response.currentWeather.temperature,
            weatherCode = response.currentWeather.weatherCode,
            windSpeed = response.currentWeather.windSpeed,
            isDay = response.currentWeather.isDay == 1
        )
    }
}

@Serializable
data class OpenMeteoResponse(
    @SerialName("current_weather") val currentWeather: CurrentWeather
)

@Serializable
data class CurrentWeather(
    val temperature: Double,
    @SerialName("weathercode") val weatherCode: Int,
    @SerialName("windspeed") val windSpeed: Double,
    @SerialName("is_day") val isDay: Int
)

data class WeatherResult(
    val temperature: Double,
    val weatherCode: Int,
    val windSpeed: Double,
    val isDay: Boolean
)
