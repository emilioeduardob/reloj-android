package com.example.relojandroid.faces

import androidx.compose.ui.graphics.Color
import com.example.relojandroid.data.Settings
import com.example.relojandroid.data.WeatherApi
import com.example.relojandroid.engine.Face
import com.example.relojandroid.engine.PixelMatrix
import com.example.relojandroid.engine.WeatherIcon
import com.example.relojandroid.engine.drawBigString
import com.example.relojandroid.engine.drawWeatherIcon
import com.example.relojandroid.engine.measureBigString

class WeatherFace(private val api: WeatherApi = WeatherApi()) : Face {
    override val id = "weather"
    override val name = "Weather"

    private var cachedResult: com.example.relojandroid.data.WeatherResult? = null
    private var lastFetch: Long = 0
    private val cacheTtlMs = 10 * 60 * 1000L // 10 minutes

    override suspend fun isAvailable(settings: Settings): Boolean = true

    override suspend fun render(settings: Settings): PixelMatrix {
        val now = System.currentTimeMillis()
        val weather = try {
            if (cachedResult == null || now - lastFetch > cacheTtlMs) {
                cachedResult = api.fetchCurrent(settings.weatherLat, settings.weatherLon)
                lastFetch = now
            }
            cachedResult!!
        } catch (e: Exception) {
            return PixelMatrix.empty()
                .drawBigString("NO DATA", 2, 1, Color(0xFFFF0000))
        }

        val tempStr = "${weather.temperature.toInt()} C"
        val icon = WeatherIcon.fromWeatherCode(weather.weatherCode, weather.isDay)

        // LaMetric layout: 8x8 icon on the left, big text on the right.
        val textX = 9
        val textY = 1
        val textWidth = measureBigString(tempStr)
        val centeredTextX = 8 + (PixelMatrix.WIDTH - 8 - textWidth) / 2

        return PixelMatrix.empty()
            .drawWeatherIcon(icon, offsetX = 0, offsetY = 0)
            .drawBigString(tempStr, centeredTextX.coerceAtLeast(textX), textY, Color(0xFFFFFF00))
    }
}
