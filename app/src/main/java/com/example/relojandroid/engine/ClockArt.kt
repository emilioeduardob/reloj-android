package com.example.relojandroid.engine

import androidx.compose.ui.graphics.Color

/**
 * Customizable pixel-art decoration shown to the left of the clock time.
 */
enum class ClockArt(val displayName: String) {
    ABSTRACT("Abstract"),
    FLOWER("Flower"),
    HEART("Heart"),
    WEATHER("Weather"),
    CALENDAR("Calendar");

    companion object {
        fun fromId(id: String): ClockArt = entries.find { it.name.equals(id, ignoreCase = true) } ?: ABSTRACT
    }
}

private data class ArtPixel(val x: Int, val y: Int, val color: Color)

private val patterns: Map<ClockArt, List<ArtPixel>> = mapOf(
    ClockArt.ABSTRACT to listOf(
        // Green base
        ArtPixel(0, 5, Color(0xFF228B22)), ArtPixel(0, 6, Color(0xFF228B22)),
        ArtPixel(1, 4, Color(0xFF228B22)), ArtPixel(1, 5, Color(0xFF32CD32)), ArtPixel(1, 6, Color(0xFF228B22)),
        ArtPixel(2, 3, Color(0xFF228B22)), ArtPixel(2, 4, Color(0xFF32CD32)), ArtPixel(2, 5, Color(0xFF32CD32)), ArtPixel(2, 6, Color(0xFF228B22)),
        ArtPixel(3, 2, Color(0xFF228B22)), ArtPixel(3, 3, Color(0xFF32CD32)), ArtPixel(3, 4, Color(0xFF32CD32)), ArtPixel(3, 5, Color(0xFF32CD32)), ArtPixel(3, 6, Color(0xFF228B22)),
        ArtPixel(4, 1, Color(0xFF228B22)), ArtPixel(4, 2, Color(0xFF32CD32)), ArtPixel(4, 3, Color(0xFF32CD32)), ArtPixel(4, 4, Color(0xFF32CD32)), ArtPixel(4, 5, Color(0xFF32CD32)), ArtPixel(4, 6, Color(0xFF228B22)),
        ArtPixel(5, 0, Color(0xFF228B22)), ArtPixel(5, 1, Color(0xFF32CD32)), ArtPixel(5, 2, Color(0xFF32CD32)), ArtPixel(5, 3, Color(0xFF32CD32)), ArtPixel(5, 4, Color(0xFF32CD32)), ArtPixel(5, 5, Color(0xFF32CD32)), ArtPixel(5, 6, Color(0xFF228B22)),
        // Blue/yellow accents
        ArtPixel(1, 1, Color(0xFF1E90FF)), ArtPixel(2, 1, Color(0xFF87CEEB)), ArtPixel(2, 2, Color(0xFF1E90FF)),
        ArtPixel(3, 0, Color(0xFF87CEEB)), ArtPixel(4, 0, Color(0xFFFFFF00)),
        ArtPixel(0, 3, Color(0xFFFFFF00)), ArtPixel(1, 3, Color(0xFFFFA500)),
        ArtPixel(0, 1, Color(0xFF1E90FF))
    ),
    ClockArt.FLOWER to listOf(
        // Petals
        ArtPixel(2, 0, Color(0xFFFF69B4)), ArtPixel(3, 0, Color(0xFFFF69B4)),
        ArtPixel(1, 1, Color(0xFFFF69B4)), ArtPixel(4, 1, Color(0xFFFF69B4)),
        ArtPixel(0, 2, Color(0xFFFF69B4)), ArtPixel(5, 2, Color(0xFFFF69B4)),
        ArtPixel(0, 3, Color(0xFFFF69B4)), ArtPixel(5, 3, Color(0xFFFF69B4)),
        ArtPixel(1, 4, Color(0xFFFF69B4)), ArtPixel(4, 4, Color(0xFFFF69B4)),
        ArtPixel(2, 5, Color(0xFFFF69B4)), ArtPixel(3, 5, Color(0xFFFF69B4)),
        // Center
        ArtPixel(2, 2, Color(0xFFFFFF00)), ArtPixel(3, 2, Color(0xFFFFFF00)),
        ArtPixel(2, 3, Color(0xFFFFFF00)), ArtPixel(3, 3, Color(0xFFFFFF00)),
        // Stem
        ArtPixel(2, 6, Color(0xFF228B22)), ArtPixel(3, 6, Color(0xFF228B22)),
        ArtPixel(2, 7, Color(0xFF228B22)), ArtPixel(3, 7, Color(0xFF228B22))
    ),
    ClockArt.HEART to listOf(
        ArtPixel(1, 0, Color(0xFFFF0000)), ArtPixel(2, 0, Color(0xFFFF0000)), ArtPixel(4, 0, Color(0xFFFF0000)), ArtPixel(5, 0, Color(0xFFFF0000)),
        ArtPixel(0, 1, Color(0xFFFF0000)), ArtPixel(1, 1, Color(0xFFFF0000)), ArtPixel(2, 1, Color(0xFFFF0000)), ArtPixel(3, 1, Color(0xFFFF0000)), ArtPixel(4, 1, Color(0xFFFF0000)), ArtPixel(5, 1, Color(0xFFFF0000)), ArtPixel(6, 1, Color(0xFFFF0000)),
        ArtPixel(0, 2, Color(0xFFFF0000)), ArtPixel(1, 2, Color(0xFFFF0000)), ArtPixel(2, 2, Color(0xFFFF0000)), ArtPixel(3, 2, Color(0xFFFF0000)), ArtPixel(4, 2, Color(0xFFFF0000)), ArtPixel(5, 2, Color(0xFFFF0000)), ArtPixel(6, 2, Color(0xFFFF0000)),
        ArtPixel(1, 3, Color(0xFFFF0000)), ArtPixel(2, 3, Color(0xFFFF0000)), ArtPixel(3, 3, Color(0xFFFF0000)), ArtPixel(4, 3, Color(0xFFFF0000)), ArtPixel(5, 3, Color(0xFFFF0000)),
        ArtPixel(2, 4, Color(0xFFFF0000)), ArtPixel(3, 4, Color(0xFFFF0000)), ArtPixel(4, 4, Color(0xFFFF0000)),
        ArtPixel(3, 5, Color(0xFFFF0000))
    ),
    ClockArt.WEATHER to listOf(
        // Sun
        ArtPixel(1, 0, Color(0xFFFFFF00)), ArtPixel(3, 0, Color(0xFFFFFF00)), ArtPixel(5, 0, Color(0xFFFFFF00)),
        ArtPixel(0, 1, Color(0xFFFFFF00)), ArtPixel(2, 1, Color(0xFFFFFF00)), ArtPixel(4, 1, Color(0xFFFFFF00)), ArtPixel(6, 1, Color(0xFFFFFF00)),
        ArtPixel(1, 2, Color(0xFFFFFF00)), ArtPixel(3, 2, Color(0xFFFFFF00)), ArtPixel(5, 2, Color(0xFFFFFF00)),
        // Cloud
        ArtPixel(1, 3, Color(0xFFAAAAAA)), ArtPixel(2, 3, Color(0xFFAAAAAA)), ArtPixel(3, 3, Color(0xFFAAAAAA)),
        ArtPixel(0, 4, Color(0xFFAAAAAA)), ArtPixel(1, 4, Color(0xFFFFFFFF)), ArtPixel(2, 4, Color(0xFFFFFFFF)), ArtPixel(3, 4, Color(0xFFFFFFFF)), ArtPixel(4, 4, Color(0xFFAAAAAA)),
        ArtPixel(0, 5, Color(0xFFAAAAAA)), ArtPixel(1, 5, Color(0xFFFFFFFF)), ArtPixel(2, 5, Color(0xFFFFFFFF)), ArtPixel(3, 5, Color(0xFFFFFFFF)), ArtPixel(4, 5, Color(0xFFAAAAAA)),
        ArtPixel(1, 6, Color(0xFFAAAAAA)), ArtPixel(2, 6, Color(0xFFAAAAAA)), ArtPixel(3, 6, Color(0xFFAAAAAA))
    )
)

fun PixelMatrix.drawClockArt(art: ClockArt, offsetX: Int, offsetY: Int): PixelMatrix {
    var result = this
    patterns[art]?.forEach { pixel ->
        result = result.set(offsetX + pixel.x, offsetY + pixel.y, pixel.color)
    }
    return result
}

/**
 * Draw an 8x8 calendar pixel-art showing the given day of month.
 * Top two rows are red; the body is white with the day number in black.
 */
fun PixelMatrix.drawCalendar(dayOfMonth: Int, offsetX: Int, offsetY: Int): PixelMatrix {
    val red = Color(0xFFFF3333)
    val white = Color(0xFFFFFFFF)
    val black = Color(0xFF000000)
    val dayStr = dayOfMonth.coerceIn(1, 31).toString()

    var result = this.mutate {
        // Red header rows.
        for (x in 0 until 8) {
            this[offsetX + x, offsetY] = red
            this[offsetX + x, offsetY + 1] = red
        }

        // White calendar page background.
        for (y in 2 until 8) {
            for (x in 0 until 8) {
                this[offsetX + x, offsetY + y] = white
            }
        }
    }

    // Day number in small 3x5 font, centered vertically in rows 2-7.
    val textY = offsetY + 2
    val textX = if (dayStr.length == 1) {
        offsetX + (8 - 3) / 2
    } else {
        offsetX
    }
    return result.drawString(dayStr, textX, textY, black)
}
