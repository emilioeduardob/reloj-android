package com.example.relojandroid.faces

import androidx.compose.ui.graphics.Color
import com.example.relojandroid.data.KanjiApi
import com.example.relojandroid.data.KanjiDetails
import com.example.relojandroid.data.Settings
import com.example.relojandroid.engine.Face
import com.example.relojandroid.engine.PixelMatrix
import com.example.relojandroid.engine.drawSampledText
import com.example.relojandroid.engine.drawSampledTextScroll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.cos
import kotlin.math.sin

class KanjiOfDayFace(private val api: KanjiApi = KanjiApi()) : Face {

    override val id = "kanji"
    override val name = "Kanji of the Day"

    // Use 4x the base density so the kanji has enough pixels to be legible.
    private val matrixWidth = PixelMatrix.WIDTH * 4
    private val matrixHeight = PixelMatrix.HEIGHT * 4
    private val kanjiSize = 32
    private val textAreaWidth = matrixWidth - kanjiSize
    private val textRowHeight = matrixHeight / 2

    private var cachedListId: String? = null
    private var cachedKanjiList: List<String>? = null
    private var cachedDetails: KanjiDetails? = null
    private var cachedDetailsDay: Int = -1
    private var cachedManualIndex: Int = -1

    // User-tap override for the day-of-year index. -1 means follow the calendar.
    private val manualIndex = AtomicInteger(-1)

    // Background loading for tap-to-refresh so the engine never blocks on network.
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val isLoading = AtomicBoolean(false)
    private var loadJob: Job? = null

    override suspend fun onTap(settings: Settings): Boolean {
        manualIndex.incrementAndGet()
        // Invalidate cached details so the next render knows we need fresh data.
        cachedDetails = null
        isLoading.set(true)

        loadJob?.cancel()
        loadJob = scope.launch {
            try {
                ensureDataLoaded(settings)
            } finally {
                isLoading.set(false)
            }
        }
        return true // Ask the engine to reset the rotation timer.
    }

    override suspend fun render(settings: Settings): PixelMatrix {
        if (isLoading.get()) {
            return renderLoading()
        }

        val listId = settings.kanjiList.ifBlank { "joyo" }

        val kanjiList = try {
            if (cachedKanjiList == null || cachedListId != listId) {
                cachedListId = listId
                cachedKanjiList = api.fetchList(listId)
                cachedDetails = null
                cachedDetailsDay = -1
                manualIndex.set(-1)
                cachedManualIndex = -1
            }
            cachedKanjiList
        } catch (e: Exception) {
            return renderError("KANJI LIST")
        }

        if (kanjiList.isNullOrEmpty()) {
            return renderError("EMPTY LIST")
        }

        val today = LocalDate.now()
        val dayOfYear = today.dayOfYear
        val override = manualIndex.get()
        if (override != cachedManualIndex) {
            cachedDetails = null
            cachedManualIndex = override
        }
        val index = if (override >= 0) override.mod(kanjiList.size) else (dayOfYear - 1).mod(kanjiList.size)
        val dailyKanji = kanjiList[index]

        val details = try {
            if (cachedDetails == null || cachedDetails?.kanji != dailyKanji || cachedDetailsDay != dayOfYear) {
                cachedDetails = api.fetchDetails(dailyKanji)
                cachedDetailsDay = dayOfYear
            }
            cachedDetails!!
        } catch (e: Exception) {
            return renderError("DETAILS")
        }

        val reading = buildReading(details)
        val meaning = buildMeaning(details)
        val scrollOffset = (System.currentTimeMillis() / 200).toInt()

        return PixelMatrix.empty(width = matrixWidth, height = matrixHeight)
            .drawSampledText(
                text = details.kanji,
                x = 0,
                y = 0,
                width = kanjiSize,
                height = matrixHeight,
                color = Color(0xFFFF5555)
            )
            .drawSampledTextScroll(
                text = reading,
                x = kanjiSize,
                y = 0,
                width = textAreaWidth,
                height = textRowHeight,
                color = Color(0xFFFFFFFF),
                scrollOffset = scrollOffset
            )
            .drawSampledTextScroll(
                text = meaning,
                x = kanjiSize,
                y = textRowHeight,
                width = textAreaWidth,
                height = textRowHeight,
                color = Color(0xFFFFFF00),
                scrollOffset = scrollOffset
            )
    }

    private suspend fun ensureDataLoaded(settings: Settings) {
        val listId = settings.kanjiList.ifBlank { "joyo" }
        if (cachedKanjiList == null || cachedListId != listId) {
            cachedListId = listId
            cachedKanjiList = api.fetchList(listId)
            cachedDetails = null
            cachedDetailsDay = -1
            manualIndex.set(-1)
            cachedManualIndex = -1
        }
        val kanjiList = cachedKanjiList ?: return
        if (kanjiList.isEmpty()) return

        val today = LocalDate.now()
        val dayOfYear = today.dayOfYear
        val override = manualIndex.get()
        if (override != cachedManualIndex) {
            cachedDetails = null
            cachedManualIndex = override
        }
        val index = if (override >= 0) override.mod(kanjiList.size) else (dayOfYear - 1).mod(kanjiList.size)
        val dailyKanji = kanjiList[index]

        if (cachedDetails == null || cachedDetails?.kanji != dailyKanji || cachedDetailsDay != dayOfYear) {
            cachedDetails = api.fetchDetails(dailyKanji)
            cachedDetailsDay = dayOfYear
        }
    }

    private fun renderLoading(): PixelMatrix {
        val frame = ((System.currentTimeMillis() / 150) % 8).toInt()
        val cx = matrixWidth / 2
        val cy = matrixHeight / 2
        val radiusX = 12.0
        val radiusY = 4.0

        return PixelMatrix.empty(width = matrixWidth, height = matrixHeight).mutate {
            for (i in 0 until 8) {
                val active = (i == frame)
                if (!active) continue
                val angle = Math.toRadians((i * 45).toDouble())
                val x = (cx + radiusX * cos(angle)).toInt()
                val y = (cy + radiusY * sin(angle)).toInt()
                if (x in 0 until matrixWidth && y in 0 until matrixHeight) {
                    this[x, y] = Color(0xFFFFFFFF)
                }
                // Glow neighbours for a softer LED look.
                val glow = Color(0xFF888888)
                if (x - 1 in 0 until matrixWidth && y in 0 until matrixHeight) this[x - 1, y] = glow
                if (x + 1 in 0 until matrixWidth && y in 0 until matrixHeight) this[x + 1, y] = glow
                if (x in 0 until matrixWidth && y - 1 in 0 until matrixHeight) this[x, y - 1] = glow
                if (x in 0 until matrixWidth && y + 1 in 0 until matrixHeight) this[x, y + 1] = glow
            }
        }
    }

    private fun buildReading(details: KanjiDetails): String {
        return details.kunReadings
            .map { it.trimHyphenNotation() }
            .firstOrNull { it.isNotBlank() }
            ?: details.onReadings.map { it.trimHyphenNotation() }.firstOrNull { it.isNotBlank() }
            ?: details.meanings.firstOrNull()
            ?: "?"
    }

    private fun buildMeaning(details: KanjiDetails): String {
        return details.meanings.firstOrNull()?.ifBlank { "?" } ?: "?"
    }

    private fun String.trimHyphenNotation(): String {
        return this.removePrefix("-").removeSuffix("-").trim()
    }

    private fun renderError(label: String): PixelMatrix {
        return PixelMatrix.empty(width = matrixWidth, height = matrixHeight)
            .drawSampledText("漢", 0, 0, kanjiSize, matrixHeight, Color(0xFFFF0000))
            .drawSampledText(label, kanjiSize, 0, textAreaWidth, textRowHeight, Color(0xFFFF0000))
    }
}
