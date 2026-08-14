package com.example.relojandroid.engine

import androidx.compose.ui.graphics.Color

/**
 * A fixed-size virtual LED grid. Pixels are stored row-major:
 * index = y * width + x.
 *
 * The matrix is immutable by default; use [mutate] to batch many pixel changes
 * into a single copy.
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

    /**
     * Set a single pixel. Prefer [mutate] when changing many pixels.
     */
    fun set(x: Int, y: Int, color: Color): PixelMatrix {
        if (x < 0 || x >= width || y < 0 || y >= height) return this
        val newPixels = pixels.toMutableList()
        newPixels[y * width + x] = color
        return copy(pixels = newPixels)
    }

    fun fill(color: Color): PixelMatrix = copy(pixels = List(width * height) { color })

    /**
     * Batch many pixel mutations into a single copy of the backing list.
     */
    fun mutate(block: Mutable.() -> Unit): PixelMatrix {
        val mutable = Mutable(width, height, pixels.toMutableList())
        mutable.block()
        return copy(pixels = mutable.pixels.toList())
    }

    /**
     * Mutable view used only inside [mutate].
     */
    class Mutable internal constructor(
        val width: Int,
        val height: Int,
        internal val pixels: MutableList<Color>
    ) {
        operator fun set(x: Int, y: Int, color: Color) {
            if (x in 0 until width && y in 0 until height) {
                pixels[y * width + x] = color
            }
        }

        fun drawRect(x: Int, y: Int, w: Int, h: Int, color: Color) {
            for (yy in y until y + h) {
                for (xx in x until x + w) {
                    this[xx, yy] = color
                }
            }
        }
    }

    companion object {
        // LaMetric TIME form factor: 37 columns × 8 rows total.
        // Leftmost 8×8 area is the color pixel-art section; the rest is the
        // monochrome main display.
        const val WIDTH = 37
        const val HEIGHT = 8

        fun empty(width: Int = WIDTH, height: Int = HEIGHT): PixelMatrix =
            PixelMatrix(width, height)
    }
}
