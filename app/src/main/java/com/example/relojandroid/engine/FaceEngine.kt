package com.example.relojandroid.engine

import androidx.compose.ui.graphics.Color
import com.example.relojandroid.data.Settings
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first

class FaceEngine(
    private val faces: List<Face>,
    private val settingsFlow: Flow<Settings>
) {
    private val _matrix = MutableStateFlow(PixelMatrix.empty())
    val matrix: StateFlow<PixelMatrix> = _matrix

    private val _currentFaceId = MutableStateFlow<String?>(null)
    val currentFaceId: StateFlow<String?> = _currentFaceId

    private val _currentFaceName = MutableStateFlow("")
    val currentFaceName: StateFlow<String> = _currentFaceName

    suspend fun run() {
        while (true) {
            val settings = settingsFlow.first()
            val enabledFaces = faces.filter { it.id in settings.enabledFaces && it.isAvailable(settings) }

            if (enabledFaces.isEmpty()) {
                _matrix.value = renderNoFaces()
                _currentFaceId.value = null
                _currentFaceName.value = ""
                delay(1000)
                continue
            }

            for (face in enabledFaces) {
                _currentFaceId.value = face.id
                _currentFaceName.value = face.name
                val faceStart = System.currentTimeMillis()
                val durationMs = settings.rotationSeconds * 1000L

                // Render face repeatedly until its turn expires.
                while (System.currentTimeMillis() - faceStart < durationMs) {
                    _matrix.value = try {
                        face.render(settings)
                    } catch (e: Exception) {
                        renderError(e.message ?: "ERR")
                    }
                    delay(200) // ~5 FPS internal refresh
                }
            }
        }
    }

    private fun renderNoFaces(): PixelMatrix {
        return PixelMatrix.empty()
            .drawString("NO FACES", 2, 12, Color(0xFFFF0000))
    }

    private fun renderError(message: String): PixelMatrix {
        return PixelMatrix.empty()
            .drawString("ERR", 2, 2, Color(0xFFFF0000))
            .drawString(message.take(15), 2, 10, Color(0xFFFF5500))
    }
}
