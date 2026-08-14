package com.example.relojandroid.data

import androidx.compose.ui.graphics.Color
import com.example.relojandroid.engine.PixelMatrix
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Parsed LaMetric icon ready for rendering.
 *
 * @property id External LaMetric icon id (string so it can be stored in settings).
 * @property name Human-readable name.
 * @property isAnimated True when the icon has more than one frame.
 * @property frames Each frame as an 8×8 [PixelMatrix].
 * @property delays Per-frame display delay in milliseconds. May be shorter than
 *           [frames]; missing entries default to [DEFAULT_FRAME_DELAY_MS].
 */
data class LaMetricIcon(
    val id: String,
    val name: String,
    val isAnimated: Boolean,
    val frames: List<PixelMatrix>,
    val delays: List<Int>
) {
    init {
        require(frames.isNotEmpty()) { "Icon must have at least one frame" }
    }

    companion object {
        const val DEFAULT_FRAME_DELAY_MS = 200
        const val WIDTH = 8
        const val HEIGHT = 8
    }
}

/**
 * Top-level response from `preloadicons?icon_id=...`.
 */
@Serializable
data class LaMetricIconDataResponse(
    val id: Int,
    val name: String,
    val type: Int,
    val body: String
)

/**
 * The double-encoded JSON string inside [LaMetricIconDataResponse.body].
 */
@Serializable
data class LaMetricIconBody(
    val icons: List<List<List<List<Float>>>> = emptyList(),
    val delays: List<Int> = emptyList()
)

/**
 * Catalog item returned by `preloadicons?page=...&category=...`.
 */
@Serializable
data class LaMetricCatalogResponse(
    val icons: List<LaMetricCatalogItem> = emptyList(),
    val count_all: Int = 0
)

@Serializable
data class LaMetricCatalogItem(
    val id: Int,
    val name: String,
    val type: Int,
    val category: String,
    val thumbnail_image: String
)

private val iconJson = Json { ignoreUnknownKeys = true; isLenient = true }

/**
 * Parse a raw icon-data response into a domain [LaMetricIcon].
 */
fun LaMetricIconDataResponse.toLaMetricIcon(): LaMetricIcon {
    val bodyParsed = iconJson.decodeFromString<LaMetricIconBody>(body)

    val frames = bodyParsed.icons.map { frame ->
        val pixels = List(LaMetricIcon.WIDTH * LaMetricIcon.HEIGHT) { Color.Black }.toMutableList()
        frame.forEachIndexed { y, row ->
            row.forEachIndexed { x, rgba ->
                if (rgba.size >= 3 && y in 0 until LaMetricIcon.HEIGHT && x in 0 until LaMetricIcon.WIDTH) {
                    val r = rgba.getOrElse(0) { 0f }
                    val g = rgba.getOrElse(1) { 0f }
                    val b = rgba.getOrElse(2) { 0f }
                    val a = rgba.getOrElse(3) { 1f }
                    if (a > 0f) {
                        pixels[y * LaMetricIcon.WIDTH + x] = colorFromFloats(r, g, b, a)
                    }
                }
            }
        }
        PixelMatrix(LaMetricIcon.WIDTH, LaMetricIcon.HEIGHT, pixels)
    }

    return LaMetricIcon(
        id = id.toString(),
        name = name,
        isAnimated = type == 1 || frames.size > 1,
        frames = frames,
        delays = bodyParsed.delays
    )
}

private fun colorFromFloats(r: Float, g: Float, b: Float, a: Float): Color =
    Color(
        red = r.coerceIn(0f, 1f),
        green = g.coerceIn(0f, 1f),
        blue = b.coerceIn(0f, 1f),
        alpha = a.coerceIn(0f, 1f)
    )
