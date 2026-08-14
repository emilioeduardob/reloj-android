package com.example.relojandroid.faces

import androidx.compose.ui.graphics.Color
import com.example.relojandroid.data.Settings
import com.example.relojandroid.engine.ClockArt
import com.example.relojandroid.engine.Face
import com.example.relojandroid.engine.PixelMatrix
import com.example.relojandroid.engine.drawBigChar
import com.example.relojandroid.engine.drawBigString
import com.example.relojandroid.engine.drawCalendar
import com.example.relojandroid.engine.drawClockArt
import com.example.relojandroid.engine.measureBigString
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class ClockFace : Face {
    override val id = "clock"
    override val name = "Clock"

    private val timeFormat = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())

    // LaMetric layout: 8x8 color art on the left, large time in the remaining 29 cols.
    private val artSize = 8
    private val mainDisplayWidth = PixelMatrix.WIDTH - artSize

    override suspend fun render(settings: Settings): PixelMatrix {
        val now = LocalDateTime.now()
        val timeStr = timeFormat.format(now)
        val art = ClockArt.fromId(settings.clockArt)

        val timeWidth = measureBigString(timeStr)
        // Center the time in the main (right) display area.
        val timeX = artSize + (mainDisplayWidth - timeWidth) / 2
        val timeY = (PixelMatrix.HEIGHT - 7) / 2

        var matrix = PixelMatrix.empty()
        matrix = if (art == ClockArt.CALENDAR) {
            matrix.drawCalendar(now.dayOfMonth, offsetX = 0, offsetY = 0)
        } else {
            matrix.drawClockArt(art, offsetX = 0, offsetY = 0)
        }
        matrix = matrix.drawBigString(timeStr, timeX, timeY, Color(0xFFFFFFFF))

        // Blinking colon: dim it every other second.
        val showColon = now.second % 2 == 0
        if (!showColon) {
            // Colon is the 3rd character (index 2) of "HH:mm".
            val colonX = timeX + measureBigString(timeStr.take(2)) + 1
            matrix = matrix.drawBigChar(':', colonX, timeY, Color(0xFF444444))
        }

        return matrix
    }
}
