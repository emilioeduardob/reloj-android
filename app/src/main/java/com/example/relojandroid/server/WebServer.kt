package com.example.relojandroid.server

import android.content.Context
import androidx.compose.ui.graphics.Color
import com.example.relojandroid.data.IconRepository
import com.example.relojandroid.data.LaMetricCatalogItem
import com.example.relojandroid.data.LaMetricIcon
import com.example.relojandroid.data.Settings
import com.example.relojandroid.data.SettingsRepository
import com.example.relojandroid.engine.Face
import com.example.relojandroid.engine.FaceEngine
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.cio.CIO
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondBytes
import io.ktor.server.response.respondText
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.format.DateTimeFormatter
import java.util.Locale

class WebServer(
    private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val iconRepository: IconRepository,
    private val faces: List<Face>,
    private val faceEngine: FaceEngine
) {
    private var server: EmbeddedServer<*, *>? = null

    fun start(port: Int) {
        stop()
        server = embeddedServer(CIO, port = port) {
            module()
        }.start(wait = false)
    }

    fun stop() {
        server?.stop(1000, 2000)
        server = null
    }

    private fun Application.module() {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true; isLenient = true })
        }
        install(StatusPages) {
            exception<CancellationException> { _, cause ->
                throw cause
            }
            exception<Throwable> { call, cause ->
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to (cause.message ?: "Unknown error")))
            }
        }

        routing {
            get("/") {
                val html = context.assets.open("web/index.html").bufferedReader().use { it.readText() }
                call.respondText(html, ContentType.Text.Html)
            }

            get("/api/settings") {
                val settings = settingsRepository.settings.first()
                call.respond(settings)
            }

            post("/api/settings") {
                val request = call.receive<Settings>()
                val validatedPattern = validateDatePattern(request.calendarDatePattern)
                settingsRepository.updateSettings(request.copy(calendarDatePattern = validatedPattern))
                call.respond(mapOf("ok" to true))
            }

            get("/api/faces") {
                val settings = settingsRepository.settings.first()
                val response = faces.map { face ->
                    FaceInfo(
                        id = face.id,
                        name = face.name,
                        enabled = face.id in settings.enabledFaces
                    )
                }
                call.respond(response)
            }

            get("/api/status") {
                val settings = settingsRepository.settings.first()
                call.respond(
                    StatusResponse(
                        currentFaceId = faceEngine.currentFaceId.value,
                        currentFaceName = faceEngine.currentFaceName.value,
                        serverPort = settings.serverPort
                    )
                )
            }

            get("/api/preview") {
                val matrix = faceEngine.matrix.value
                call.respond(
                    PreviewResponse(
                        width = matrix.width,
                        height = matrix.height,
                        pixels = matrix.pixels.map { colorToHex(it) }
                    )
                )
            }

            iconRoutes()
        }
    }

    private fun io.ktor.server.routing.Route.iconRoutes() {
        get("/api/icons/categories") {
            call.respond(ICON_CATEGORIES)
        }

        get("/api/icons") {
            val category = call.request.queryParameters["category"] ?: ""
            val search = call.request.queryParameters["search"] ?: ""
            val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 0
            val count = call.request.queryParameters["count"]?.toIntOrNull() ?: 80

            val result = iconRepository.searchIcons(category, search, page, count)
            call.respond(
                IconSearchResponse(
                    icons = result.icons.map { it.toApiModel() },
                    total = result.count_all
                )
            )
        }

        get("/api/icons/{id}") {
            val id = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing id"))
            val icon = iconRepository.getIcon(id)
            if (icon == null) {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "Icon not found"))
            } else {
                call.respond(icon.toApiModel())
            }
        }

        get("/api/icons/{id}/thumbnail") {
            val id = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing id"))
            val relativePath = call.request.queryParameters["path"]
                ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing path"))

            try {
                val bytes = iconRepository.fetchThumbnail(relativePath)
                call.respondBytes(bytes, ContentType.Image.PNG)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                call.respond(HttpStatusCode.NotFound)
            }
        }

        post("/api/icons/select") {
            val request = call.receive<SelectIconRequest>()
            val target = request.target?.lowercase()
            val iconId = request.iconId
            if (target.isNullOrBlank() || target !in ICON_TARGETS) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid target"))
            } else if (iconId.isNullOrBlank()) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing iconId"))
            } else {
                try {
                    // Download and cache the icon before saving the setting so the face
                    // can render it immediately.
                    iconRepository.fetchAndCache(iconId)
                    val settings = settingsRepository.settings.first()
                    settingsRepository.updateSettings(
                        settings.withIcon(target, iconId, request.thumbnailPath)
                    )
                    call.respond(mapOf("ok" to true))
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadGateway, mapOf("error" to (e.message ?: "Failed to download icon")))
                }
            }
        }

        delete("/api/icons/selected") {
            val target = call.request.queryParameters["target"]?.lowercase() ?: ""
            if (target !in ICON_TARGETS) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid target"))
            } else {
                val settings = settingsRepository.settings.first()
                settingsRepository.updateSettings(settings.withIcon(target, null, null))
                call.respond(mapOf("ok" to true))
            }
        }

        get("/api/icons/selected") {
            val target = call.request.queryParameters["target"]?.lowercase() ?: ""
            if (target !in ICON_TARGETS) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid target"))
            } else {
                val settings = settingsRepository.settings.first()
                val iconId = settings.iconIdFor(target)
                if (iconId == null) {
                    call.respond(SelectedIconResponse(selected = false, icon = null))
                } else {
                    val icon = iconRepository.getIcon(iconId)
                    if (icon == null) {
                        call.respond(SelectedIconResponse(selected = false, icon = null))
                    } else {
                        val thumbnailPath = settings.iconThumbnailPathFor(target)
                        call.respond(
                            SelectedIconResponse(
                                selected = true,
                                icon = icon.toApiModel(thumbnailPath ?: "")
                            )
                        )
                    }
                }
            }
        }
    }

    private fun Settings.iconIdFor(target: String): String? = when (target) {
        "clock" -> clockIconId
        "exchange" -> exchangeIconId
        "calendar" -> calendarIconId
        else -> null
    }

    private fun Settings.iconThumbnailPathFor(target: String): String? = when (target) {
        "clock" -> clockIconThumbnailPath
        "exchange" -> exchangeIconThumbnailPath
        "calendar" -> calendarIconThumbnailPath
        else -> null
    }

    private fun Settings.withIcon(target: String, iconId: String?, thumbnailPath: String?): Settings = when (target) {
        "clock" -> copy(clockIconId = iconId, clockIconThumbnailPath = thumbnailPath)
        "exchange" -> copy(exchangeIconId = iconId, exchangeIconThumbnailPath = thumbnailPath)
        "calendar" -> copy(calendarIconId = iconId, calendarIconThumbnailPath = thumbnailPath)
        else -> this
    }

    private fun colorToHex(color: Color): String {
        val r = (color.red * 255).toInt()
        val g = (color.green * 255).toInt()
        val b = (color.blue * 255).toInt()
        return String.format("#%02X%02X%02X", r, g, b)
    }

    private fun validateDatePattern(pattern: String): String {
        val trimmed = pattern.trim()
        if (trimmed.isBlank()) return "dd/MM"
        return try {
            DateTimeFormatter.ofPattern(trimmed, Locale.getDefault())
            trimmed
        } catch (e: IllegalArgumentException) {
            "dd/MM"
        }
    }

    companion object {
        private val ICON_TARGETS = setOf("clock", "exchange", "calendar")
        private val ICON_CATEGORIES = listOf(
            IconCategory("", "All"),
            IconCategory("cartoons_movies", "Cartoons & Movies"),
            IconCategory("characters", "Characters"),
            IconCategory("sports", "Sports"),
            IconCategory("flags", "Flags"),
            IconCategory("weather", "Weather"),
            IconCategory("transport", "Transport"),
            IconCategory("food", "Food"),
            IconCategory("animals", "Animals"),
            IconCategory("notifications", "Notifications"),
            IconCategory("games", "Games"),
            IconCategory("misc", "Misc")
        )
    }
}

@Serializable
data class FaceInfo(
    val id: String,
    val name: String,
    val enabled: Boolean
)

@Serializable
data class StatusResponse(
    val currentFaceId: String?,
    val currentFaceName: String,
    val serverPort: Int
)

@Serializable
data class PreviewResponse(
    val width: Int,
    val height: Int,
    val pixels: List<String>
)

@Serializable
data class IconCategory(
    val id: String,
    val name: String
)

@Serializable
data class IconSearchResponse(
    val icons: List<IconInfo>,
    val total: Int
)

@Serializable
data class IconInfo(
    val id: String,
    val name: String,
    val animated: Boolean,
    val category: String,
    val thumbnailPath: String
)

@Serializable
data class SelectIconRequest(
    val target: String?,
    val iconId: String?,
    val thumbnailPath: String? = null
)

@Serializable
data class SelectedIconResponse(
    val selected: Boolean,
    val icon: IconInfo?
)

private fun LaMetricCatalogItem.toApiModel(): IconInfo = IconInfo(
    id = id.toString(),
    name = name,
    animated = type == 1,
    category = category,
    thumbnailPath = thumbnail_image
)

private fun LaMetricIcon.toApiModel(thumbnailPath: String = ""): IconInfo = IconInfo(
    id = id,
    name = name,
    animated = isAnimated,
    category = "",
    thumbnailPath = thumbnailPath
)
