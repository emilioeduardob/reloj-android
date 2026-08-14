package com.example.relojandroid.faces

import androidx.compose.ui.graphics.Color
import com.example.relojandroid.data.ExchangeApi
import com.example.relojandroid.data.IconRepository
import com.example.relojandroid.data.LaMetricIcon
import com.example.relojandroid.data.Settings
import com.example.relojandroid.engine.Face
import com.example.relojandroid.engine.PixelMatrix
import com.example.relojandroid.engine.drawBigString
import com.example.relojandroid.engine.drawIcon
import com.example.relojandroid.engine.drawString
import com.example.relojandroid.engine.frameAt
import com.example.relojandroid.engine.measureBigString
import kotlinx.coroutines.CancellationException

class ExchangeFace(
    private val api: ExchangeApi = ExchangeApi(),
    private val iconRepository: IconRepository
) : Face {
    override val id = "exchange"
    override val name = "Dólar PYG"

    private var cachedSource: String? = null
    private var cachedResult: com.example.relojandroid.data.ExchangeResult? = null
    private var lastFetch: Long = 0
    private val cacheTtlMs = 5 * 60 * 1000L // 5 minutes

    private var cachedIconId: String? = null
    private var cachedIcon: LaMetricIcon? = null

    override suspend fun isAvailable(settings: Settings): Boolean = true

    override suspend fun render(settings: Settings): PixelMatrix {
        val now = System.currentTimeMillis()
        val exchange = try {
            if (cachedResult == null || cachedSource != settings.exchangeSource || now - lastFetch > cacheTtlMs) {
                cachedResult = api.fetchUsdPyg(settings.exchangeSource)
                cachedSource = settings.exchangeSource
                lastFetch = now
            }
            cachedResult!!
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            return PixelMatrix.empty()
                .drawString("NO DATA", 2, 2, Color(0xFFFF0000))
        }

        // Alternate between buy and sell every 3 seconds.
        val showBuy = (now / 3000) % 2 == 0L
        val label = if (showBuy) "C" else "V"
        val rate = if (showBuy) exchange.buy else exchange.sell
        val line = "$label${formatRate(rate)}"

        val artSize = 8
        var matrix = PixelMatrix.empty()
        val icon = settings.exchangeIconId?.let { loadIcon(it) }

        if (icon != null) {
            val frame = icon.frameAt(now)
            matrix = matrix.drawIcon(frame, offsetX = 0, offsetY = 0)
        }

        val textWidth = measureBigString(line)
        val textX = if (icon != null) {
            artSize + (PixelMatrix.WIDTH - artSize - textWidth) / 2
        } else {
            (PixelMatrix.WIDTH - textWidth) / 2
        }

        return matrix.drawBigString(line, textX, 1, Color(0xFFFFFFFF))
    }

    override suspend fun isAnimated(settings: Settings): Boolean {
        return loadIcon(settings.exchangeIconId ?: return false)?.isAnimated == true
    }

    private suspend fun loadIcon(iconId: String): LaMetricIcon? {
        if (cachedIconId == iconId && cachedIcon != null) {
            return cachedIcon
        }
        cachedIcon = iconRepository.getIcon(iconId)
        cachedIconId = iconId
        return cachedIcon
    }

    private fun formatRate(rate: Double): String {
        return rate.toInt().toString()
    }
}
