package com.example.relojandroid.server

import android.content.Context
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
import io.ktor.server.engine.embeddedServer
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class WebServer(
    private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val faces: List<Face>,
    private val engine: FaceEngine
) {
    private var server: io.ktor.server.engine.ApplicationEngine? = null

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
            exception<Throwable> { call, cause ->
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to (cause.message ?: "Unknown error"))
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
                settingsRepository.updateSettings(request)
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
                        currentFaceId = engine.currentFaceId.value,
                        currentFaceName = engine.currentFaceName.value,
                        serverPort = settings.serverPort
                    )
                )
            }

            get("/api/preview") {
                val matrix = engine.matrix.value
                call.respond(
                    PreviewResponse(
                        width = matrix.width,
                        height = matrix.height,
                        pixels = matrix.pixels.map { colorToHex(it) }
                    )
                )
            }
        }
    }

    private fun colorToHex(color: androidx.compose.ui.graphics.Color): String {
        val r = (color.red * 255).toInt()
        val g = (color.green * 255).toInt()
        val b = (color.blue * 255).toInt()
        return String.format("#%02X%02X%02X", r, g, b)
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
