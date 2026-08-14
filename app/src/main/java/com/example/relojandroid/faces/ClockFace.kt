package com.example.relojandroid.faces

import androidx.compose.ui.graphics.Color
import com.example.relojandroid.data.Settings
import com.example.relojandroid.engine.Face
import com.example.relojandroid.engine.PixelMatrix
import com.example.relojandroid.engine.drawChar
import com.example.relojandroid.engine.drawString
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class ClockFace : Face {
    override val id = "clock"
    override val name = "Clock"

    private val timeFormat = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())
    private val dateFormat = DateTimeFormatter.ofPattern("EEE d MMM", Locale.getDefault())

    override suspend fun render(settings: Settings): PixelMatrix {
        val now = LocalDateTime.now()
        val timeStr = timeFormat.format(now)
        val dateStr = dateFormat.format(now).uppercase(Locale.getDefault())

        val timeWidth = timeStr.length * 4 - 1
        val dateWidth = dateStr.length * 4 - 1

        val color = Color(0xFF00FF00)
        val dimColor = Color(0xFF004400)

        var matrix = PixelMatrix.empty()
            .drawString(timeStr, (PixelMatrix.WIDTH - timeWidth) / 2, 6, color)
            .drawString(dateStr, (PixelMatrix.WIDTH - dateWidth) / 2, 20, dimColor)

        // Blinking colon effect: dim the colon every other second.
        if (now.second % 2 != 0) {
            val colonX = (PixelMatrix.WIDTH - timeWidth) / 2 + 2 * 4
            matrix = matrix.drawChar(':', colonX, 6, dimColor)
        }

        return matrix
    }
}
