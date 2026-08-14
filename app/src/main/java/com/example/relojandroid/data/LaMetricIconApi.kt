package com.example.relojandroid.data

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.HttpResponse
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

class LaMetricIconApi {

    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true; isLenient = true })
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 30_000
            connectTimeoutMillis = 10_000
        }
    }

    /**
     * Search the LaMetric icon catalog.
     *
     * @param category Category slug, e.g. "cartoons_movies".
     * @param search Free-text search query.
     * @param page Zero-based page index.
     * @param count Items per page.
     */
    suspend fun searchIcons(
        category: String = "",
        search: String = "",
        page: Int = 0,
        count: Int = 80
    ): LaMetricCatalogResponse {
        return client.get("$BASE_URL/api/v1/dev/preloadicons") {
            parameter("page", page)
            parameter("category", category)
            parameter("search", search)
            parameter("count", count)
            parameter("guest_icons", "")
        }.body()
    }

    /**
     * Fetch the raw JSON document for an icon so it can be cached verbatim.
     */
    suspend fun fetchIconDataRaw(iconId: String): String {
        return client.get("$BASE_URL/api/v1/dev/preloadicons") {
            parameter("icon_id", iconId)
        }.body<String>()
    }

    /**
     * Fetch a thumbnail image as raw bytes. Caller supplies the relative path
     * returned by the catalog (e.g. "/content/apps/icon_thumbs/3049_icon_thumb.png").
     */
    suspend fun fetchThumbnail(relativePath: String): ByteArray {
        val response: HttpResponse = client.get("$BASE_URL$relativePath")
        return response.body<ByteArray>()
    }

    companion object {
        private const val BASE_URL = "https://faces.lametric.com"
    }
}
