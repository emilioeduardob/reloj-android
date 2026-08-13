package com.example.relojandroid.engine

import androidx.compose.ui.graphics.Color
import com.example.relojandroid.data.Settings
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

class FaceEngine(
    private val faces: List<Face>,
    private val settingsFlow: Flow<Settings>
) {
    private val _matrix = MutableStateFlow(PixelMatrix.empty())
    val matrix: StateFlow<PixelMatrix> = _matrix

    private val _currentFaceId = MutableStateFlow<String?>(null)
    val currentFaceId: StateFlow<String?> = _currentFaceId

    private val _currentFaceName = MutableStateFlow<String>("")
    val currentFaceName: StateFlow<String> = _currentFaceName

    suspend fun run() {
        combine(settingsFlow, tickFlow()) { settings, _ -> settings }
            .collect { settings ->
                val enabledFaces = faces.filter { it.id in settings.enabledFaces && it.isAvailable(settings) }
                if (enabledFaces.isEmpty()) {
                    _matrix.value = renderNoFaces()
                    _currentFaceId.value = null
                    _currentFaceName.value = ""
                    delay(1000)
                    return@collect
                }

                for (face in enabledFaces) {
                    _currentFaceId.value = face.id
                    _currentFaceName.value = face.name
                    val faceStart = System.currentTimeMillis()
                    val durationMs = settings.rotationSeconds * 1000L

                    // Render face repeatedly until its turn expires.
                    while (System.currentTimeMillis() - faceStart < durationMs) {
                        try {
                            _matrix.value = face.render(settings)
                        } catch (e: Exception) {
                            _matrix.value = renderError(e.message ?: "ERR")
                        }
                        delay(200) // ~5 FPS internal refresh
                    }
                }
            }
    }

    private fun tickFlow(): Flow<Long> = flow {
        while (true) {
            emit(System.currentTimeMillis())
            delay(200)
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
