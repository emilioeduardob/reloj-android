package com.example.relojandroid.faces

import androidx.compose.ui.graphics.Color
import com.example.relojandroid.data.Settings
import com.example.relojandroid.data.WeatherApi
import com.example.relojandroid.engine.Face
import com.example.relojandroid.engine.PixelMatrix
import com.example.relojandroid.engine.drawString

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
                .drawString("NO DATA", 2, 2, Color(0xFFFF0000))
        }

        val tempStr = "${weather.temperature.toInt()} C"
        val matrix = PixelMatrix.empty()

        // Small weather icon on the left, temperature on the right.
        return drawIcon(matrix, weather.weatherCode, weather.isDay, 1, 1)
            .drawString(tempStr, 9, 2, Color(0xFFFFFF00))
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
        val center = listOf(Offset(2, 2))
        val rays = listOf(
            Offset(2, 0), Offset(4, 2), Offset(2, 4), Offset(0, 2),
            Offset(1, 1), Offset(3, 1), Offset(1, 3), Offset(3, 3)
        )
        (center + rays).forEach { m = m.set(x + it.x, y + it.y, yellow) }
        return m
    }

    private fun drawCloud(matrix: PixelMatrix, x: Int, y: Int): PixelMatrix {
        val gray = Color(0xFFAAAAAA)
        var m = matrix
        val pixels = listOf(
            Offset(1, 2), Offset(2, 1), Offset(3, 1), Offset(4, 2),
            Offset(4, 3), Offset(3, 3), Offset(2, 3), Offset(1, 3)
        )
        pixels.forEach { m = m.set(x + it.x, y + it.y, gray) }
        return m
    }

    private fun drawRain(matrix: PixelMatrix, x: Int, y: Int): PixelMatrix {
        val blue = Color(0xFF00AAFF)
        var m = drawCloud(matrix, x, y)
        listOf(Offset(1, 4), Offset(3, 4)).forEach {
            m = m.set(x + it.x, y + it.y, blue)
        }
        return m
    }

    private fun drawSnow(matrix: PixelMatrix, x: Int, y: Int): PixelMatrix {
        val white = Color(0xFFFFFFFF)
        var m = drawCloud(matrix, x, y)
        listOf(Offset(0, 4), Offset(2, 4), Offset(4, 4)).forEach {
            m = m.set(x + it.x, y + it.y, white)
        }
        return m
    }

    private fun drawStorm(matrix: PixelMatrix, x: Int, y: Int): PixelMatrix {
        val yellow = Color(0xFFFFFF00)
        var m = drawCloud(matrix, x, y)
        listOf(Offset(2, 4), Offset(3, 4)).forEach {
            m = m.set(x + it.x, y + it.y, yellow)
        }
        return m
    }

    private data class Offset(val x: Int, val y: Int)
}
