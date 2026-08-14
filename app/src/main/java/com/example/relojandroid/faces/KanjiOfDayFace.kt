package com.example.relojandroid.faces

import androidx.compose.ui.graphics.Color
import com.example.relojandroid.data.KanjiApi
import com.example.relojandroid.data.KanjiDetails
import com.example.relojandroid.data.Settings
import com.example.relojandroid.engine.Face
import com.example.relojandroid.engine.PixelMatrix
import com.example.relojandroid.engine.drawSampledText
import com.example.relojandroid.engine.drawSampledTextScroll
import java.time.LocalDate

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

    override suspend fun render(settings: Settings): PixelMatrix {
        val listId = settings.kanjiList.ifBlank { "joyo" }

        val kanjiList = try {
            if (cachedKanjiList == null || cachedListId != listId) {
                cachedListId = listId
                cachedKanjiList = api.fetchList(listId)
                cachedDetails = null
                cachedDetailsDay = -1
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
        val index = (dayOfYear - 1) % kanjiList.size
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

    private fun buildReading(details: KanjiDetails): String {
        val readings = mutableListOf<String>()
        readings += details.kunReadings.map { it.trimHyphenNotation() }
        readings += details.onReadings.map { it.trimHyphenNotation() }

        val joined = readings.filter { it.isNotBlank() }.distinct()
        if (joined.isNotEmpty()) {
            return joined.joinToString("  ")
        }
        return details.meanings.firstOrNull() ?: "?"
    }

    private fun buildMeaning(details: KanjiDetails): String {
        return details.meanings.joinToString("  ").ifBlank { "?" }
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
