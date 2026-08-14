package com.example.relojandroid.data

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class ExchangeApi {

    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true; isLenient = true })
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 15_000
            connectTimeoutMillis = 10_000
        }
    }

    suspend fun fetchUsdPyg(source: String = "bcp"): ExchangeResult {
        val response: DolarPyResponse = client.get(
            "http://dolar.melizeche.com/api/1.0/"
        ).body()

        val provider = response.dolarpy[source]
            ?: response.dolarpy["bcp"]
            ?: throw IllegalStateException("No exchange provider available")

        return ExchangeResult(
            buy = provider.compra,
            sell = provider.venta,
            source = source
        )
    }
}

@Serializable
data class DolarPyResponse(
    val dolarpy: Map<String, DolarPyProvider>,
    val updated: String? = null
)

@Serializable
data class DolarPyProvider(
    val compra: Double,
    val venta: Double,
    @SerialName("referencial_diario") val referencialDiario: Double? = null
)

data class ExchangeResult(
    val buy: Double,
    val sell: Double,
    val source: String
)
