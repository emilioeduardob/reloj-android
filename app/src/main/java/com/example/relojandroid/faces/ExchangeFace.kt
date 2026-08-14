package com.example.relojandroid.faces

import androidx.compose.ui.graphics.Color
import com.example.relojandroid.data.ExchangeApi
import com.example.relojandroid.data.Settings
import com.example.relojandroid.engine.Face
import com.example.relojandroid.engine.PixelMatrix
import com.example.relojandroid.engine.drawString

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
                .drawString("NO DATA", 2, 2, Color(0xFFFF0000))
        }

        // Alternate between buy and sell every 3 seconds on a single line.
        val showBuy = (now / 3000) % 2 == 0L
        val label = if (showBuy) "C" else "V"
        val rate = if (showBuy) exchange.buy else exchange.sell
        val line = "$label:${formatRate(rate)}"

        return PixelMatrix.empty()
            .drawString(line, 2, 2, Color(0xFFFFFF00))
    }

    private fun formatRate(rate: Double): String {
        return rate.toInt().toString()
    }
}
