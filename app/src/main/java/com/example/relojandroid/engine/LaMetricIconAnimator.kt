package com.example.relojandroid.engine

import androidx.compose.ui.graphics.Color
import com.example.relojandroid.data.LaMetricIcon

/**
 * Blit an 8×8 icon frame onto a larger matrix at the given offset.
 */
fun PixelMatrix.drawIcon(icon: PixelMatrix, offsetX: Int, offsetY: Int): PixelMatrix {
    val newPixels = pixels.toMutableList()
    for (y in 0 until icon.height) {
        for (x in 0 until icon.width) {
            val color = icon[x, y]
            if (color != Color.Black) {
                val px = offsetX + x
                val py = offsetY + y
                if (px in 0 until width && py in 0 until height) {
                    newPixels[py * width + px] = color
                }
            }
        }
    }
    return copy(pixels = newPixels)
}

/**
 * Pick the frame that should be displayed at [elapsedTimeMs] for a LaMetric icon.
 * Timing loops forever using the cumulative frame delays.
 */
fun LaMetricIcon.frameAt(elapsedTimeMs: Long): PixelMatrix {
    if (!isAnimated || frames.size == 1) return frames.first()

    val totalDuration = frames.indices.sumOf { index ->
        delays.getOrNull(index)?.toLong() ?: LaMetricIcon.DEFAULT_FRAME_DELAY_MS.toLong()
    }.coerceAtLeast(1L)

    var remaining = elapsedTimeMs % totalDuration
    var frameIndex = 0
    while (frameIndex < frames.size - 1) {
        val delay = delays.getOrNull(frameIndex) ?: LaMetricIcon.DEFAULT_FRAME_DELAY_MS
        if (remaining < delay) break
        remaining -= delay
        frameIndex++
    }
    return frames[frameIndex]
}
