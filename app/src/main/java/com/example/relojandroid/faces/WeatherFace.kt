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

        // Single-line layout: 8x8 icon on the left, temperature on the right.
        return drawIcon(matrix, weather.weatherCode, weather.isDay, 0, 0)
            .drawString(tempStr, 9, 2, Color(0xFFFFFF00))
    }

    private fun drawIcon(
        matrix: PixelMatrix,
        code: Int,
        isDay: Boolean,
        x: Int,
        y: Int
    ): PixelMatrix {
        val icon = when (code) {
            0 -> if (isDay) WeatherIcon.SUN else WeatherIcon.MOON
            1, 2 -> if (isDay) WeatherIcon.PARTLY_CLOUDY else WeatherIcon.PARTLY_CLOUDY_NIGHT
            3 -> WeatherIcon.CLOUD
            45, 48 -> WeatherIcon.CLOUD
            51, 53, 55 -> WeatherIcon.RAINDROP
            61, 63, 65 -> WeatherIcon.RAIN_CLOUD
            71, 73, 75 -> WeatherIcon.SNOWFLAKE
            80, 81, 82 -> WeatherIcon.RAIN_CLOUD2
            95, 96, 99 -> WeatherIcon.LIGHTNING
            else -> if (isDay) WeatherIcon.SUN else WeatherIcon.MOON
        }
        return drawWeatherIcon(matrix, x, y, icon)
    }

    private fun drawWeatherIcon(
        matrix: PixelMatrix,
        x: Int,
        y: Int,
        icon: WeatherIcon
    ): PixelMatrix {
        val white = Color(0xFFFFFFFF)
        val yellow = Color(0xFFFFFF00)
        return matrix.mutate {
            icon.pixels.forEachIndexed { row, cols ->
                cols.forEachIndexed { col, value ->
                    val color = when (value) {
                        1 -> white
                        2 -> yellow
                        else -> null
                    }
                    if (color != null) {
                        this[x + col, y + row] = color
                    }
                }
            }
        }
    }
}

/**
 * 8x8 pixel weather icons.
 * Values: 0 = transparent, 1 = white, 2 = yellow.
 */
private enum class WeatherIcon(val pixels: List<List<Int>>) {
    SUN(
        listOf(
            listOf(2, 0, 0, 0, 0, 0, 0, 2),
            listOf(0, 0, 0, 1, 1, 0, 0, 0),
            listOf(0, 0, 2, 2, 2, 2, 0, 0),
            listOf(0, 1, 2, 2, 2, 2, 1, 0),
            listOf(0, 1, 2, 2, 2, 2, 1, 0),
            listOf(0, 0, 2, 2, 2, 2, 0, 0),
            listOf(0, 0, 0, 1, 1, 0, 0, 0),
            listOf(2, 0, 0, 0, 0, 0, 0, 2)
        )
    ),
    MOON(
        listOf(
            listOf(0, 0, 0, 0, 0, 0, 0, 0),
            listOf(0, 0, 0, 2, 2, 2, 0, 0),
            listOf(0, 2, 2, 2, 0, 0, 0, 0),
            listOf(0, 2, 2, 0, 0, 0, 0, 0),
            listOf(0, 2, 2, 0, 0, 0, 0, 0),
            listOf(0, 2, 2, 2, 0, 0, 0, 0),
            listOf(0, 0, 0, 2, 2, 2, 0, 0),
            listOf(0, 0, 0, 0, 0, 0, 0, 0)
        )
    ),
    CLOUD(
        listOf(
            listOf(0, 0, 0, 0, 0, 0, 0, 0),
            listOf(0, 0, 0, 1, 1, 0, 0, 0),
            listOf(0, 0, 1, 1, 1, 0, 0, 0),
            listOf(1, 1, 1, 1, 1, 1, 1, 1),
            listOf(1, 1, 1, 1, 1, 1, 1, 1),
            listOf(1, 1, 1, 1, 1, 1, 1, 1),
            listOf(0, 0, 1, 1, 1, 1, 0, 0),
            listOf(0, 0, 0, 0, 0, 0, 0, 0)
        )
    ),
    PARTLY_CLOUDY(
        listOf(
            listOf(0, 0, 1, 0, 0, 0, 0, 0),
            listOf(0, 0, 1, 0, 0, 0, 0, 0),
            listOf(0, 0, 1, 1, 1, 2, 2, 1),
            listOf(0, 0, 1, 1, 1, 1, 2, 2),
            listOf(1, 1, 1, 1, 1, 1, 1, 1),
            listOf(1, 1, 1, 1, 1, 1, 1, 1),
            listOf(1, 1, 1, 1, 1, 1, 1, 1),
            listOf(0, 0, 0, 1, 1, 1, 0, 0)
        )
    ),
    PARTLY_CLOUDY_NIGHT(
        listOf(
            listOf(0, 0, 0, 0, 0, 2, 2, 2),
            listOf(0, 0, 0, 0, 2, 2, 2, 2),
            listOf(0, 0, 1, 1, 2, 2, 0, 0),
            listOf(0, 1, 1, 1, 1, 2, 2, 0),
            listOf(1, 1, 1, 1, 1, 1, 1, 2),
            listOf(1, 1, 1, 1, 1, 1, 1, 1),
            listOf(1, 1, 1, 1, 1, 1, 1, 1),
            listOf(0, 1, 1, 1, 1, 1, 0, 0)
        )
    ),
    RAIN_CLOUD(
        listOf(
            listOf(0, 0, 0, 0, 0, 0, 0, 0),
            listOf(0, 0, 0, 1, 1, 0, 0, 0),
            listOf(0, 0, 1, 1, 1, 0, 0, 0),
            listOf(1, 1, 1, 1, 1, 1, 1, 1),
            listOf(1, 1, 1, 1, 1, 1, 1, 1),
            listOf(1, 1, 1, 1, 1, 1, 1, 1),
            listOf(0, 0, 1, 1, 1, 1, 0, 0),
            listOf(0, 0, 1, 0, 0, 1, 0, 0)
        )
    ),
    RAIN_CLOUD2(
        listOf(
            listOf(0, 0, 0, 0, 0, 0, 0, 0),
            listOf(0, 0, 0, 1, 1, 0, 0, 0),
            listOf(0, 0, 1, 1, 1, 0, 0, 0),
            listOf(1, 1, 1, 1, 1, 1, 1, 1),
            listOf(1, 1, 1, 1, 1, 1, 1, 1),
            listOf(1, 1, 1, 1, 1, 1, 1, 1),
            listOf(0, 1, 0, 1, 0, 1, 0, 0),
            listOf(0, 1, 0, 1, 0, 1, 0, 0)
        )
    ),
    SNOWFLAKE(
        listOf(
            listOf(0, 0, 0, 1, 1, 0, 0, 0),
            listOf(0, 1, 0, 0, 0, 0, 1, 0),
            listOf(0, 0, 0, 0, 0, 0, 0, 0),
            listOf(1, 1, 0, 1, 1, 0, 1, 1),
            listOf(1, 1, 0, 1, 1, 0, 1, 1),
            listOf(0, 0, 0, 0, 0, 0, 0, 0),
            listOf(0, 1, 0, 1, 1, 0, 1, 0),
            listOf(0, 0, 0, 1, 1, 0, 0, 0)
        )
    ),
    LIGHTNING(
        listOf(
            listOf(0, 0, 0, 0, 2, 0, 0, 0),
            listOf(0, 0, 0, 2, 2, 0, 0, 0),
            listOf(0, 0, 1, 2, 0, 0, 0, 0),
            listOf(0, 0, 2, 2, 2, 2, 0, 0),
            listOf(0, 0, 2, 2, 2, 2, 0, 0),
            listOf(0, 0, 0, 0, 2, 0, 0, 0),
            listOf(0, 0, 0, 2, 2, 0, 0, 0),
            listOf(0, 0, 0, 2, 0, 0, 0, 0)
        )
    ),
    RAINDROP(
        listOf(
            listOf(0, 0, 0, 0, 0, 0, 0, 0),
            listOf(0, 0, 0, 1, 1, 0, 0, 0),
            listOf(0, 0, 0, 1, 1, 0, 0, 0),
            listOf(0, 0, 1, 1, 1, 1, 0, 0),
            listOf(0, 1, 1, 1, 1, 1, 0, 0),
            listOf(0, 1, 1, 1, 1, 1, 0, 0),
            listOf(0, 0, 1, 1, 1, 1, 0, 0),
            listOf(0, 0, 0, 1, 1, 0, 0, 0)
        )
    )
}
