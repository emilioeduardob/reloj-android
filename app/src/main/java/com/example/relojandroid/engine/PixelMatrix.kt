package com.example.relojandroid.engine

import androidx.compose.ui.graphics.Color

/**
 * A fixed-size virtual LED grid. Pixels are stored row-major:
 * index = y * width + x.
 */
data class PixelMatrix(
    val width: Int,
    val height: Int,
    val pixels: List<Color> = List(width * height) { Color.Black }
) {
    init {
        require(pixels.size == width * height) {
            "Pixel list size (${pixels.size}) must equal width*height ($width * $height = ${width * height})"
        }
    }

    operator fun get(x: Int, y: Int): Color {
        if (x < 0 || x >= width || y < 0 || y >= height) return Color.Black
        return pixels[y * width + x]
    }

    fun set(x: Int, y: Int, color: Color): PixelMatrix {
        if (x < 0 || x >= width || y < 0 || y >= height) return this
        val newPixels = pixels.toMutableList()
        newPixels[y * width + x] = color
        return copy(pixels = newPixels)
    }

    fun fill(color: Color): PixelMatrix = copy(pixels = List(width * height) { color })

    fun drawRect(x: Int, y: Int, w: Int, h: Int, color: Color): PixelMatrix {
        var result = this
        for (yy in y until y + h) {
            for (xx in x until x + w) {
                result = result.set(xx, yy, color)
            }
        }
        return result
    }

    companion object {
        const val WIDTH = 64
        const val HEIGHT = 32

        fun empty(width: Int = WIDTH, height: Int = HEIGHT): PixelMatrix =
            PixelMatrix(width, height)
    }
}
