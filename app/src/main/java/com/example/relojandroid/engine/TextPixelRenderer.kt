package com.example.relojandroid.engine

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import androidx.compose.ui.graphics.Color

/**
 * Render arbitrary text (e.g. kanji, hiragana) into a low-resolution pixel
 * rectangle by drawing it to a small bitmap and sampling the pixels.
 *
 * This is intentionally separate from the 3x5 / 5x7 ASCII fonts so any Unicode
 * character returned by an API can be displayed on the dot matrix.
 */
fun PixelMatrix.drawSampledText(
    text: String,
    x: Int,
    y: Int,
    width: Int,
    height: Int,
    color: Color,
    threshold: Float = 0.35f
): PixelMatrix {
    if (text.isBlank() || width <= 0 || height <= 0) return this

    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = android.graphics.Color.WHITE
        textAlign = Paint.Align.LEFT
    }

    // Find the largest text size that fits the requested rectangle.
    val bounds = Rect()
    var low = 1f
    var high = (height * 3f).coerceAtLeast(8f)
    repeat(12) {
        val mid = (low + high) / 2f
        paint.textSize = mid
        paint.getTextBounds(text, 0, text.length, bounds)
        val fits = bounds.width() <= width && bounds.height() <= height
        if (fits) low = mid else high = mid
    }
    paint.textSize = low
    paint.getTextBounds(text, 0, text.length, bounds)

    // Center the text in the bitmap.
    val drawX = (width - bounds.width()) / 2f - bounds.left
    val drawY = (height + bounds.height()) / 2f - bounds.bottom
    canvas.drawText(text, drawX, drawY, paint)

    // Sample the bitmap back into the matrix.
    val thresholdInt = (threshold * 255).toInt()
    return mutate {
        for (yy in 0 until height) {
            for (xx in 0 until width) {
                val pixel = bitmap.getPixel(xx, yy)
                val alpha = android.graphics.Color.alpha(pixel)
                if (alpha >= thresholdInt) {
                    this[x + xx, y + yy] = color
                }
            }
        }
    }
}

/**
 * Render text fitted to [height] and sample a sliding [width]×[height] window
 * from the rendered text. [scrollOffset] is the pixel position of the left
 * edge of the window; it wraps around automatically so the text loops.
 *
 * Use this for long reading strings on the narrow main display.
 */
fun PixelMatrix.drawSampledTextScroll(
    text: String,
    x: Int,
    y: Int,
    width: Int,
    height: Int,
    color: Color,
    scrollOffset: Int,
    gap: Int = 8,
    threshold: Float = 0.35f
): PixelMatrix {
    if (text.isBlank() || width <= 0 || height <= 0) return this

    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = android.graphics.Color.WHITE
        textAlign = Paint.Align.LEFT
    }

    // Size the text to fit the height but allow it to be as wide as needed.
    val bounds = Rect()
    var low = 1f
    var high = (height * 3f).coerceAtLeast(8f)
    repeat(12) {
        val mid = (low + high) / 2f
        paint.textSize = mid
        paint.getTextBounds(text, 0, text.length, bounds)
        val fitsHeight = bounds.height() <= height
        if (fitsHeight) low = mid else high = mid
    }
    paint.textSize = low
    paint.getTextBounds(text, 0, text.length, bounds)

    val textWidth = bounds.width()
    if (textWidth <= width) {
        // The whole text fits; just draw it centered without scrolling.
        return drawSampledText(text, x, y, width, height, color, threshold)
    }

    val totalWidth = textWidth + gap
    val offset = ((scrollOffset % totalWidth) + totalWidth) % totalWidth
    // Make the bitmap wide enough for two copies so the window can wrap seamlessly.
    val bitmapWidth = totalWidth + width
    val bitmap = Bitmap.createBitmap(bitmapWidth, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val drawX = -bounds.left.toFloat()
    val drawY = (height + bounds.height()) / 2f - bounds.bottom
    canvas.drawText(text, drawX, drawY, paint)
    // Repeat the text after the gap for seamless looping.
    canvas.drawText(text, drawX + totalWidth, drawY, paint)

    val thresholdInt = (threshold * 255).toInt()
    return mutate {
        for (yy in 0 until height) {
            for (xx in 0 until width) {
                val srcX = offset + xx
                if (srcX >= bitmapWidth) continue
                val pixel = bitmap.getPixel(srcX.toInt(), yy)
                val alpha = android.graphics.Color.alpha(pixel)
                if (alpha >= thresholdInt) {
                    this[x + xx, y + yy] = color
                }
            }
        }
    }
}
