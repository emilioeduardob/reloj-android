package com.example.relojandroid.faces

import androidx.compose.ui.graphics.Color
import com.example.relojandroid.data.ExchangeApi
import com.example.relojandroid.data.Settings
import com.example.relojandroid.engine.Face
import com.example.relojandroid.engine.PixelMatrix
import com.example.relojandroid.engine.drawBigString
import com.example.relojandroid.engine.drawCoinIcon

class ExchangeFace(private val api: ExchangeApi = ExchangeApi()) : Face {
    override val id = "exchange"
    override val name = "Dólar PYG"

    private var cachedResult: com.example.relojandroid.data.ExchangeResult? = null
    private var lastFetch: Long = 0
    private val cacheTtlMs = 5 * 60 * 1000L // 5 minutes

    override suspend fun isAvailable(settings: Settings): Boolean = true

    override suspend fun render(settings: Settings): PixelMatrix {
        val now = System.currentTimeMillis()
        val exchange = try {
            if (cachedResult == null || now - lastFetch > cacheTtlMs) {
                cachedResult = api.fetchUsdPyg(settings.exchangeSource)
                lastFetch = now
            }
            cachedResult!!
        } catch (e: Exception) {
            return PixelMatrix.empty()
                .drawBigString("NO DATA", 2, 1, Color(0xFFFFFFFF))
        }

        // Alternate between buy and sell every 3 seconds.
        val label = if ((now / 3000) % 2 == 0L) "C" else "V"
        val rate = if ((now / 3000) % 2 == 0L) exchange.buy else exchange.sell
        val line = "$label${formatRate(rate)}"

        // LaMetric layout: 8x8 coin icon on the left, big white rate on the right.
        return PixelMatrix.empty()
            .drawCoinIcon(offsetX = 0, offsetY = 0)
            .drawBigString(line, 9, 1, Color(0xFFFFFFFF))
    }

    private fun formatRate(rate: Double): String {
        return rate.toInt().toString()
    }
}
