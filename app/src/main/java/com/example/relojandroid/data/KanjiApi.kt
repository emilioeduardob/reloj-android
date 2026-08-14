package com.example.relojandroid.data

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.http.encodeURLPathPart
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class KanjiApi {

    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true; isLenient = true })
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 15_000
            connectTimeoutMillis = 10_000
        }
    }

    /**
     * Fetch the list of kanji characters for a given set.
     * Supported list ids include: joyo, grade-1 .. grade-6, jlpt-5 .. jlpt-1.
     */
    suspend fun fetchList(listId: String): List<String> {
        return client.get("https://kanjiapi.dev/v1/kanji/${listId.encodeURLPathPart()}")
            .body()
    }

    /**
     * Fetch detailed information for a single kanji character.
     */
    suspend fun fetchDetails(kanji: String): KanjiDetails {
        return client.get(
            "https://kanjiapi.dev/v1/kanji/${kanji.encodeURLPathPart()}"
        ).body()
    }
}

@Serializable
data class KanjiDetails(
    val kanji: String,
    val grade: Int? = null,
    @SerialName("stroke_count") val strokeCount: Int? = null,
    val meanings: List<String> = emptyList(),
    @SerialName("kun_readings") val kunReadings: List<String> = emptyList(),
    @SerialName("on_readings") val onReadings: List<String> = emptyList(),
    @SerialName("name_readings") val nameReadings: List<String> = emptyList(),
    val jlpt: Int? = null,
    val unicode: String? = null,
    @SerialName("heisig_en") val heisigEn: String? = null
)
