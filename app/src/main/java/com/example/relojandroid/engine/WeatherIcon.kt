package com.example.relojandroid.engine

import androidx.compose.ui.graphics.Color

/**
 * 8x8 pixel-art weather icons drawn in the color section of the display.
 */
enum class WeatherIcon {
    CLEAR_DAY,
    CLEAR_NIGHT,
    PARTLY_CLOUDY_DAY,
    PARTLY_CLOUDY_NIGHT,
    CLOUDY,
    RAIN,
    SNOW,
    STORM,
    FOG;

    companion object {
        fun fromWeatherCode(code: Int, isDay: Boolean): WeatherIcon {
            return when (code) {
                0 -> if (isDay) CLEAR_DAY else CLEAR_NIGHT
                1, 2 -> if (isDay) PARTLY_CLOUDY_DAY else PARTLY_CLOUDY_NIGHT
                3 -> CLOUDY
                45, 48 -> FOG
                51, 53, 55, 61, 63, 65, 80, 81, 82 -> RAIN
                71, 73, 75 -> SNOW
                95, 96, 99 -> STORM
                else -> if (isDay) CLEAR_DAY else CLEAR_NIGHT
            }
        }
    }
}

private val yellow = Color(0xFFFFFF00)
private val white = Color(0xFFFFFFFF)
private val lightGray = Color(0xFFCCCCCC)
private val gray = Color(0xFFAAAAAA)
private val blue = Color(0xFF00AAFF)

private val patterns: Map<WeatherIcon, List<String>> = mapOf(
    WeatherIcon.CLEAR_DAY to listOf(
        "...YY...",
        ".YYYYYY.",
        "YYYYYYYY",
        "YYYYYYYY",
        "YYYYYYYY",
        ".YYYYYY.",
        "...YY...",
        "........"
    ),
    WeatherIcon.CLEAR_NIGHT to listOf(
        "....YYYY",
        ".....YYY",
        "......YY",
        "......YY",
        "......YY",
        ".....YYY",
        "....YYYY",
        "........"
    ),
    WeatherIcon.PARTLY_CLOUDY_DAY to listOf(
        "...YY...",
        ".YYYYYY.",
        "..YYYY..",
        ".YYYYYY.",
        "YYYYYYYY",
        ".YYYYYY.",
        "..YYYY..",
        "........"
    ),
    WeatherIcon.PARTLY_CLOUDY_NIGHT to listOf(
        "....YYYY",
        "...YYYYY",
        "..YYYY..",
        "..YYYYYY",
        "..YYYYYY",
        "...YYYY.",
        "....YY..",
        "........"
    ),
    WeatherIcon.CLOUDY to listOf(
        "..WWWW..",
        ".WWWWWW.",
        "WWWWWWWW",
        "WWWWWWWW",
        ".WWWWWW.",
        "..WWWW..",
        "........",
        "........"
    ),
    WeatherIcon.RAIN to listOf(
        "..GGGG..",
        ".GGGGGG.",
        "GGGGGGGG",
        "GGGGGGGG",
        "..B..B..",
        ".B....B.",
        "........",
        "........"
    ),
    WeatherIcon.SNOW to listOf(
        "..GGGG..",
        ".GGGGGG.",
        "GGGGGGGG",
        "GGGGGGGG",
        "...WW...",
        "..W..W..",
        "...WW...",
        "........"
    ),
    WeatherIcon.STORM to listOf(
        "..GGGG..",
        ".GGGGGG.",
        "GGGGGGGG",
        "GGGGGGGG",
        "...YY...",
        "..YYY...",
        ".YYY....",
        ".YY....."
    ),
    WeatherIcon.FOG to listOf(
        "........",
        ".GGGGGG.",
        ".GGGGGG.",
        "........",
        ".GGGGGG.",
        "........",
        ".GGGGGG.",
        "........"
    )
)

private fun colorFor(char: Char): Color? = when (char) {
    'Y' -> yellow
    'W' -> white
    'G' -> gray
    'B' -> blue
    else -> null
}

fun PixelMatrix.drawWeatherIcon(
    icon: WeatherIcon,
    offsetX: Int,
    offsetY: Int
): PixelMatrix {
    var result = this
    patterns[icon]?.forEachIndexed { row, line ->
        line.forEachIndexed { col, char ->
            colorFor(char)?.let { color ->
                result = result.set(offsetX + col, offsetY + row, color)
            }
        }
    }
    return result
}
