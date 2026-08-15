package com.example.relojandroid.engine

import androidx.compose.ui.graphics.Color
import com.example.relojandroid.data.Settings
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

class FaceEngine(
    private val faces: List<Face>,
    private val settingsFlow: Flow<Settings>,
    private val scope: CoroutineScope
) {

    companion object {
        private const val DEFAULT_REFRESH_MS = 200L
        private const val ANIMATED_REFRESH_MS = 50L
        private const val SWIPE_COOLDOWN_MS = 600L
    }

    private val _matrix = MutableStateFlow(PixelMatrix.empty())
    val matrix: StateFlow<PixelMatrix> = _matrix

    private val _currentFaceId = MutableStateFlow<String?>(null)
    val currentFaceId: StateFlow<String?> = _currentFaceId

    private val _currentFaceName = MutableStateFlow("")
    val currentFaceName: StateFlow<String> = _currentFaceName

    private val navigationChannel = Channel<NavigationCommand>(Channel.CONFLATED)

    suspend fun run() {
        var currentIndex = 0

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

            currentIndex = currentIndex.mod(enabledFaces.size)
            val face = enabledFaces[currentIndex]
            val durationMs = settings.rotationSeconds * 1000L
            val faceStart = System.currentTimeMillis()
            val animated = face.isAnimated(settings)
            val refreshMs = if (animated) ANIMATED_REFRESH_MS else DEFAULT_REFRESH_MS

            _currentFaceId.value = face.id
            _currentFaceName.value = face.name

            var command: NavigationCommand? = null
            while (System.currentTimeMillis() - faceStart < durationMs && command == null) {
                _matrix.value = try {
                    face.render(settings)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    renderError()
                }

                command = withTimeoutOrNull(refreshMs) { navigationChannel.receive() }
            }

            currentIndex = when (command) {
                NavigationCommand.NEXT -> (currentIndex + 1).mod(enabledFaces.size)
                NavigationCommand.PREVIOUS -> (currentIndex - 1).mod(enabledFaces.size)
                null -> (currentIndex + 1).mod(enabledFaces.size)
            }
        }
    }

    fun nextFace() {
        navigationChannel.trySend(NavigationCommand.NEXT)
    }

    fun previousFace() {
        navigationChannel.trySend(NavigationCommand.PREVIOUS)
    }

    fun onTap() {
        scope.launch {
            val settings = settingsFlow.first()
            val enabledFaces = faces.filter { it.id in settings.enabledFaces && it.isAvailable(settings) }
            val currentId = _currentFaceId.value
            val face = enabledFaces.find { it.id == currentId } ?: return@launch
            try {
                face.onTap(settings)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Ignore tap errors so the engine keeps running.
            }
        }
    }

    private fun renderNoFaces(): PixelMatrix {
        return PixelMatrix.empty()
            .drawString("NO FACES", 2, 1, Color(0xFFFF0000))
    }

    private fun renderError(): PixelMatrix {
        return PixelMatrix.empty()
            .drawString("ERR", 2, 1, Color(0xFFFF0000))
    }

    private enum class NavigationCommand { NEXT, PREVIOUS }
}
