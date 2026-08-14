package com.example.relojandroid.faces

import androidx.compose.ui.graphics.Color
import com.example.relojandroid.data.IconRepository
import com.example.relojandroid.data.LaMetricIcon
import com.example.relojandroid.data.Settings
import com.example.relojandroid.engine.ClockArt
import com.example.relojandroid.engine.Face
import com.example.relojandroid.engine.PixelMatrix
import com.example.relojandroid.engine.drawBigString
import com.example.relojandroid.engine.drawClockArt
import com.example.relojandroid.engine.drawIcon
import com.example.relojandroid.engine.frameAt
import com.example.relojandroid.engine.measureBigString
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class CalendarFace(
    private val iconRepository: IconRepository
) : Face {
    override val id = "calendar"
    override val name = "Calendar"

    private val artSize = 8
    private val mainDisplayWidth = PixelMatrix.WIDTH - artSize

    private var cachedIconId: String? = null
    private var cachedIcon: LaMetricIcon? = null

    private var cachedPattern: String? = null
    private var cachedFormatter: DateTimeFormatter? = null

    override suspend fun render(settings: Settings): PixelMatrix {
        val now = LocalDateTime.now()
        val pattern = settings.calendarDatePattern.takeIf { it.isNotBlank() } ?: "dd/MM"
        val formatter = formatterFor(pattern)
        val dateStr = try {
            formatter.format(now)
        } catch (e: Exception) {
            // Invalid pattern or unsupported field; fall back to a safe default.
            DateTimeFormatter.ofPattern("dd/MM", Locale.getDefault()).format(now)
        }

        val textWidth = measureBigString(dateStr)
        val textX = artSize + (mainDisplayWidth - textWidth) / 2
        val textY = (PixelMatrix.HEIGHT - 7) / 2

        var matrix = PixelMatrix.empty()
        val icon = settings.calendarIconId?.let { loadIcon(it) }

        if (icon != null) {
            val frame = icon.frameAt(System.currentTimeMillis())
            matrix = matrix.drawIcon(frame, offsetX = 0, offsetY = 0)
        } else {
            matrix = matrix.drawClockArt(ClockArt.ABSTRACT, offsetX = 0, offsetY = 0)
        }

        return matrix.drawBigString(dateStr, textX, textY, Color(0xFFFFFFFF))
    }

    override suspend fun isAnimated(settings: Settings): Boolean {
        return loadIcon(settings.calendarIconId ?: return false)?.isAnimated == true
    }

    private fun formatterFor(pattern: String): DateTimeFormatter {
        if (cachedPattern == pattern && cachedFormatter != null) {
            return cachedFormatter!!
        }
        val formatter = DateTimeFormatter.ofPattern(pattern, Locale.getDefault())
        cachedPattern = pattern
        cachedFormatter = formatter
        return formatter
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
