package com.example.relojandroid.data

import android.content.Context
import android.util.Log
import kotlinx.serialization.json.Json
import java.io.File
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap

class IconRepository(
    private val context: Context,
    private val api: LaMetricIconApi
) {
    private val memoryCache = ConcurrentHashMap<String, LaMetricIcon>()
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private val iconsDir: File
        get() = File(context.filesDir, "icons").also { it.mkdirs() }

    /**
     * Return a cached icon, or fetch and cache it if it isn't stored locally.
     * Returns null when the icon cannot be loaded and the network is unavailable.
     */
    suspend fun getIcon(iconId: String): LaMetricIcon? {
        memoryCache[iconId]?.let { return it }

        val file = iconFile(iconId)
        if (file.exists()) {
            try {
                val raw = file.readText()
                val parsed = json.decodeFromString<LaMetricIconDataResponse>(raw).toLaMetricIcon()
                memoryCache[iconId] = parsed
                return parsed
            } catch (e: Exception) {
                Log.w(TAG, "Failed to parse cached icon $iconId", e)
                file.delete()
            }
        }

        return try {
            fetchAndCache(iconId)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to fetch icon $iconId", e)
            null
        }
    }

    /**
     * Fetch icon data from the API and store it locally.
     */
    suspend fun fetchAndCache(iconId: String): LaMetricIcon {
        val raw = api.fetchIconDataRaw(iconId)
        val parsed = json.decodeFromString<LaMetricIconDataResponse>(raw).toLaMetricIcon()

        try {
            iconFile(iconId).writeText(raw)
        } catch (e: IOException) {
            Log.w(TAG, "Failed to cache icon $iconId", e)
        }

        memoryCache[iconId] = parsed
        return parsed
    }

    /**
     * Search the LaMetric catalog through the device proxy.
     */
    suspend fun searchIcons(
        category: String = "",
        search: String = "",
        page: Int = 0,
        count: Int = 80
    ): LaMetricCatalogResponse {
        return api.searchIcons(category, search, page, count)
    }

    /**
     * Proxy a thumbnail image so the web UI doesn't hit CORS/mixed-content issues.
     */
    suspend fun fetchThumbnail(relativePath: String): ByteArray {
        return api.fetchThumbnail(relativePath)
    }

    private fun iconFile(iconId: String): File = File(iconsDir, "$iconId.json")

    companion object {
        private const val TAG = "IconRepository"
    }
}
