package com.example.relojandroid.faces

import androidx.compose.ui.graphics.Color
import com.example.relojandroid.data.IconRepository
import com.example.relojandroid.data.LaMetricIcon
import com.example.relojandroid.data.Settings
import com.example.relojandroid.engine.ClockArt
import com.example.relojandroid.engine.Face
import com.example.relojandroid.engine.PixelMatrix
import com.example.relojandroid.engine.drawBigChar
import com.example.relojandroid.engine.drawBigString
import com.example.relojandroid.engine.drawClockArt
import com.example.relojandroid.engine.drawIcon
import com.example.relojandroid.engine.frameAt
import com.example.relojandroid.engine.measureBigString
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class ClockFace(
    private val iconRepository: IconRepository
) : Face {
    override val id = "clock"
    override val name = "Clock"

    private val timeFormat = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())

    // LaMetric layout: 8×8 color art on the left, large time in the remaining 29 cols.
    private val artSize = 8
    private val mainDisplayWidth = PixelMatrix.WIDTH - artSize

    private var cachedIconId: String? = null
    private var cachedIcon: LaMetricIcon? = null

    override suspend fun render(settings: Settings): PixelMatrix {
        val now = LocalDateTime.now()
        val timeStr = timeFormat.format(now)

        val timeWidth = measureBigString(timeStr)
        // Center the time in the main (right) display area.
        val timeX = artSize + (mainDisplayWidth - timeWidth) / 2
        val timeY = (PixelMatrix.HEIGHT - 7) / 2

        var matrix = PixelMatrix.empty()
        val icon = settings.clockIconId?.let { loadIcon(it) }

        if (icon != null) {
            val frame = icon.frameAt(System.currentTimeMillis())
            matrix = matrix.drawIcon(frame, offsetX = 0, offsetY = 0)
        } else {
            val art = ClockArt.fromId(settings.clockArt)
            matrix = matrix.drawClockArt(art, offsetX = 0, offsetY = 0)
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

    override suspend fun isAnimated(settings: Settings): Boolean {
        return loadIcon(settings.clockIconId ?: return false)?.isAnimated == true
    }

    private suspend fun loadIcon(iconId: String): LaMetricIcon? {
        if (cachedIconId == iconId && cachedIcon != null) {
            return cachedIcon
        }
        cachedIcon = iconRepository.getIcon(iconId)
        cachedIconId = iconId
        return cachedIcon
    }
}
