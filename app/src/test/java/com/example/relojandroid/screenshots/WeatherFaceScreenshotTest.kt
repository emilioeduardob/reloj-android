package com.example.relojandroid.screenshots

import android.graphics.Bitmap
import androidx.compose.ui.graphics.Color
import com.example.relojandroid.engine.PixelMatrix
import com.example.relojandroid.engine.WeatherIcon
import com.example.relojandroid.engine.drawBigString
import com.example.relojandroid.engine.drawWeatherIcon
import com.example.relojandroid.engine.measureBigString
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

/**
 * Generates PNG screenshots of the Weather face for all 9 conditions.
 * Outputs to app/build/screenshots/weather/
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class WeatherFaceScreenshotTest {

    private val outputDir = File("build/screenshots/weather").apply { mkdirs() }

    @Test
    fun generateAll() {
        generate("clear_day", WeatherIcon.CLEAR_DAY, "25 C")
        generate("clear_night", WeatherIcon.CLEAR_NIGHT, "18 C")
        generate("partly_cloudy_day", WeatherIcon.PARTLY_CLOUDY_DAY, "22 C")
        generate("partly_cloudy_night", WeatherIcon.PARTLY_CLOUDY_NIGHT, "17 C")
        generate("cloudy", WeatherIcon.CLOUDY, "20 C")
        generate("rain", WeatherIcon.RAIN, "19 C")
        generate("snow", WeatherIcon.SNOW, "-3 C")
        generate("storm", WeatherIcon.STORM, "21 C")
        generate("fog", WeatherIcon.FOG, "15 C")
    }

    private fun generate(name: String, icon: WeatherIcon, temp: String) {
        val textWidth = measureBigString(temp)
        val centeredTextX = 8 + (PixelMatrix.WIDTH - 8 - textWidth) / 2
        val matrix = PixelMatrix.empty()
            .drawWeatherIcon(icon, offsetX = 0, offsetY = 0)
            .drawBigString(temp, centeredTextX.coerceAtLeast(9), 1, Color(0xFFFFFFFF))

        val scale = 32
        val width = matrix.width * scale
        val height = matrix.height * scale
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        // Fill background black
        for (y in 0 until height) {
            for (x in 0 until width) {
                bitmap.setPixel(x, y, android.graphics.Color.BLACK)
            }
        }

        for (y in 0 until matrix.height) {
            for (x in 0 until matrix.width) {
                val color = matrix[x, y]
                if (color == Color.Black) continue

                val px = x * scale
                val py = y * scale
                val argb = composeColorToArgb(color)

                // Simple square pixel with slight rounding effect via alpha feather
                for (dy in 0 until scale) {
                    for (dx in 0 until scale) {
                        val bx = px + dx
                        val by = py + dy
                        if (bx >= width || by >= height) continue

                        // Bezel / gap
                        val gap = (scale * 0.10f).toInt()
                        if (dx < gap || dx >= scale - gap || dy < gap || dy >= scale - gap) {
                            // Dark gray bezel area
                            bitmap.setPixel(bx, by, android.graphics.Color.argb(255, 58, 58, 58))
                            continue
                        }

                        // Glow (outer ring)
                        val glowThickness = (scale * 0.06f).toInt().coerceAtLeast(1)
                        val isGlowEdge = dx < gap + glowThickness || dx >= scale - gap - glowThickness ||
                                         dy < gap + glowThickness || dy >= scale - gap - glowThickness

                        if (isGlowEdge) {
                            val alpha = (0.22f * 255).toInt()
                            val glowColor = android.graphics.Color.argb(
                                alpha,
                                android.graphics.Color.red(argb),
                                android.graphics.Color.green(argb),
                                android.graphics.Color.blue(argb)
                            )
                            bitmap.setPixel(bx, by, glowColor)
                            continue
                        }

                        // Main pixel
                        bitmap.setPixel(bx, by, argb)
                    }
                }
            }
        }

        val file = File(outputDir, "weather_$name.png")
        file.outputStream().use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        println("Screenshot saved: ${file.absolutePath}")
    }

    private fun composeColorToArgb(color: Color): Int {
        return android.graphics.Color.argb(
            (color.alpha * 255).toInt(),
            (color.red * 255).toInt(),
            (color.green * 255).toInt(),
            (color.blue * 255).toInt()
        )
    }
}
