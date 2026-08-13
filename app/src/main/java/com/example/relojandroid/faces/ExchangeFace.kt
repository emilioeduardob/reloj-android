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
                .drawString("DOLAR", 2, 2, Color(0xFFFF0000))
                .drawString("NO DATA", 2, 12, Color(0xFFFF5500))
        }

        val buyStr = "C:${formatRate(exchange.buy)}"
        val sellStr = "V:${formatRate(exchange.sell)}"

        return PixelMatrix.empty()
            .drawString("USD/PYG", 2, 2, Color(0xFF00FF00))
            .drawString(buyStr, 2, 12, Color(0xFFFFFF00))
            .drawString(sellStr, 2, 22, Color(0xFFFFAA00))
    }

    private fun formatRate(rate: Double): String {
        return rate.toInt().toString()
    }
}
