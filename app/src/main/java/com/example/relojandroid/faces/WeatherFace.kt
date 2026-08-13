package com.example.relojandroid.faces

import androidx.compose.ui.graphics.Color
import com.example.relojandroid.data.Settings
import com.example.relojandroid.data.WeatherApi
import com.example.relojandroid.engine.Face
import com.example.relojandroid.engine.PixelMatrix
import com.example.relojandroid.engine.drawString

private fun String.normalize(): String {
    return this
        .replace("á", "a").replace("Á", "A")
        .replace("é", "e").replace("É", "E")
        .replace("í", "i").replace("Í", "I")
        .replace("ó", "o").replace("Ó", "O")
        .replace("ú", "u").replace("Ú", "U")
        .replace("ñ", "n").replace("Ñ", "N")
        .replace("ü", "u").replace("Ü", "U")
}

class WeatherFace(private val api: WeatherApi = WeatherApi()) : Face {
    override val id = "weather"
    override val name = "Weather"

    private var cachedResult: com.example.relojandroid.data.WeatherResult? = null
    private var lastFetch: Long = 0
    private val cacheTtlMs = 10 * 60 * 1000L // 10 minutes

    override suspend fun isAvailable(settings: Settings): Boolean {
        return true
    }

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
                .drawString("WEATHER", 2, 2, Color(0xFFFF0000))
                .drawString("NO DATA", 2, 12, Color(0xFFFF5500))
        }

        val tempStr = "${weather.temperature.toInt()}°C"
        val cityStr = settings.weatherCity.normalize().uppercase().take(10)
        val codeStr = weatherCodeLabel(weather.weatherCode)

        var matrix = PixelMatrix.empty()
            .drawString(cityStr, 2, 2, Color(0xFF00AAFF))
            .drawString(tempStr, 2, 12, Color(0xFFFFFF00))
            .drawString(codeStr, 2, 22, Color(0xFF00FF00))

        // Small weather icon on the right side.
        matrix = drawIcon(matrix, weather.weatherCode, weather.isDay, 46, 8)

        return matrix
    }

    private fun weatherCodeLabel(code: Int): String {
        return when (code) {
            0 -> "CLEAR"
            1, 2, 3 -> "CLOUDY"
            45, 48 -> "FOG"
            51, 53, 55 -> "DRIZZLE"
            61, 63, 65 -> "RAIN"
            71, 73, 75 -> "SNOW"
            80, 81, 82 -> "SHOWERS"
            95, 96, 99 -> "STORM"
            else -> "?"
        }.take(9)
    }

    private fun drawIcon(
        matrix: PixelMatrix,
        code: Int,
        isDay: Boolean,
        x: Int,
        y: Int
    ): PixelMatrix {
        return when {
            code == 0 && isDay -> drawSun(matrix, x, y)
            code in listOf(1, 2, 3) || !isDay -> drawCloud(matrix, x, y)
            code in listOf(51, 53, 55, 61, 63, 65, 80, 81, 82) -> drawRain(matrix, x, y)
            code in listOf(71, 73, 75) -> drawSnow(matrix, x, y)
            code in listOf(95, 96, 99) -> drawStorm(matrix, x, y)
            else -> drawSun(matrix, x, y)
        }
    }

    private fun drawSun(matrix: PixelMatrix, x: Int, y: Int): PixelMatrix {
        val yellow = Color(0xFFFFFF00)
        var m = matrix
        // 5x5 sun: center + rays
        val center = listOf(Offset(2, 2))
        val rays = listOf(
            Offset(2, 0), Offset(4, 2), Offset(2, 4), Offset(0, 2),
            Offset(1, 1), Offset(3, 1), Offset(1, 3), Offset(3, 3)
        )
        (center + rays).forEach { m = m.set(x + it.x.toInt(), y + it.y.toInt(), yellow) }
        return m
    }

    private fun drawCloud(matrix: PixelMatrix, x: Int, y: Int): PixelMatrix {
        val gray = Color(0xFFAAAAAA)
        var m = matrix
        val pixels = listOf(
            Offset(1, 2), Offset(2, 1), Offset(3, 1), Offset(4, 2),
            Offset(4, 3), Offset(3, 3), Offset(2, 3), Offset(1, 3)
        )
        pixels.forEach { m = m.set(x + it.x.toInt(), y + it.y.toInt(), gray) }
        return m
    }

    private fun drawRain(matrix: PixelMatrix, x: Int, y: Int): PixelMatrix {
        val blue = Color(0xFF00AAFF)
        var m = drawCloud(matrix, x, y)
        listOf(Offset(1, 4), Offset(3, 4)).forEach {
            m = m.set(x + it.x.toInt(), y + it.y.toInt(), blue)
        }
        return m
    }

    private fun drawSnow(matrix: PixelMatrix, x: Int, y: Int): PixelMatrix {
        val white = Color(0xFFFFFFFF)
        var m = drawCloud(matrix, x, y)
        listOf(Offset(0, 4), Offset(2, 4), Offset(4, 4)).forEach {
            m = m.set(x + it.x.toInt(), y + it.y.toInt(), white)
        }
        return m
    }

    private fun drawStorm(matrix: PixelMatrix, x: Int, y: Int): PixelMatrix {
        val yellow = Color(0xFFFFFF00)
        var m = drawCloud(matrix, x, y)
        listOf(Offset(2, 4), Offset(3, 4)).forEach {
            m = m.set(x + it.x.toInt(), y + it.y.toInt(), yellow)
        }
        return m
    }

    private data class Offset(val x: Int, val y: Int)
}
