package com.example.relojandroid.faces

import androidx.compose.ui.graphics.Color
import com.example.relojandroid.data.Settings
import com.example.relojandroid.engine.ClockArt
import com.example.relojandroid.engine.Face
import com.example.relojandroid.engine.PixelMatrix
import com.example.relojandroid.engine.drawBigChar
import com.example.relojandroid.engine.drawBigString
import com.example.relojandroid.engine.drawClockArt
import com.example.relojandroid.engine.measureBigString
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class ClockFace : Face {
    override val id = "clock"
    override val name = "Clock"

    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    override suspend fun render(settings: Settings): PixelMatrix {
        val now = Calendar.getInstance()
        val timeStr = timeFormat.format(now.time)
        val art = ClockArt.fromId(settings.clockArt)

        val timeWidth = measureBigString(timeStr)

        // Center the big time vertically; place it on the right side.
        val timeX = PixelMatrix.WIDTH - timeWidth - 4
        val timeY = (PixelMatrix.HEIGHT - 7) / 2

        // Center the art vertically on the left side.
        val artY = (PixelMatrix.HEIGHT - 8) / 2

        var matrix = PixelMatrix.empty()
            .drawClockArt(art, offsetX = 4, offsetY = artY)
            .drawBigString(timeStr, timeX, timeY, Color(0xFFFFFFFF))

        // Blinking colon: dim it every other second.
        val showColon = now.get(Calendar.SECOND) % 2 == 0
        if (!showColon) {
            // Colon is the 3rd character (index 2) of "HH:mm".
            val colonX = timeX + measureBigString(timeStr.take(2)) + 1
            matrix = matrix.drawBigChar(':', colonX, timeY, Color(0xFF444444))
        }

        return matrix
    }
}
