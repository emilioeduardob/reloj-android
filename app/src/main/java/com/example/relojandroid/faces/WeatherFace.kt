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

        // 8x8 weather icon on the right side.
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
        var result = matrix
        icon.pixels.forEachIndexed { row, cols ->
            cols.forEachIndexed { col, value ->
                val color = when (value) {
                    1 -> white
                    2 -> yellow
                    else -> null
                }
                if (color != null) {
                    result = result.set(x + col, y + row, color)
                }
            }
        }
        return result
    }
}

/**
 * 8x8 pixel weather icons extracted from the provided reference sheet.
 * Values: 0 = transparent, 1 = white, 2 = yellow.
 */
private enum class WeatherIcon(val pixels: List<List<Int>>) {
    SUN(
        listOf(
            listOf(2, 0, 0, 0, 0, 0, 0, 2),
            listOf(0, 1, 0, 1, 1, 0, 1, 0),
            listOf(0, 0, 2, 2, 2, 2, 0, 0),
            listOf(0, 1, 2, 2, 2, 2, 1, 0),
            listOf(0, 1, 2, 2, 2, 2, 1, 0),
            listOf(0, 0, 2, 2, 2, 2, 0, 0),
            listOf(0, 1, 0, 1, 1, 0, 1, 0),
            listOf(2, 0, 0, 0, 0, 0, 0, 2)
        )
    ),
    MOON(
        listOf(
            listOf(0, 0, 0, 0, 0, 0, 0, 0),
            listOf(0, 0, 1, 2, 2, 2, 1, 0),
            listOf(0, 2, 2, 2, 0, 0, 0, 0),
            listOf(0, 2, 2, 0, 0, 0, 0, 0),
            listOf(0, 2, 2, 0, 0, 0, 0, 0),
            listOf(0, 2, 2, 2, 0, 0, 0, 0),
            listOf(0, 0, 1, 2, 2, 2, 1, 0),
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
            listOf(0, 1, 1, 1, 1, 2, 1, 0),
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
            listOf(0, 0, 1, 1, 1, 1, 0, 0),
            listOf(0, 0, 1, 0, 0, 1, 0, 0)
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
