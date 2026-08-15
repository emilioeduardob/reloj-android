package com.example.relojandroid.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.relojandroid.engine.PixelMatrix
import kotlinx.coroutines.flow.StateFlow
import android.view.WindowManager
import kotlin.math.abs

@Composable
fun PixelClockScreen(
    matrixFlow: StateFlow<PixelMatrix>,
    onSwipeLeft: () -> Unit = {},
    onSwipeRight: () -> Unit = {},
    onTap: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val matrix by matrixFlow.collectAsStateWithLifecycle()

    // Keep screen on and immersive while this screen is visible.
    KeepScreenOn()

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        PixelCanvas(
            matrix = matrix,
            onSwipeLeft = onSwipeLeft,
            onSwipeRight = onSwipeRight,
            onTap = onTap,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
private fun PixelCanvas(
    matrix: PixelMatrix,
    onSwipeLeft: () -> Unit,
    onSwipeRight: () -> Unit,
    onTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    var totalX = 0f
                    var totalY = 0f
                    var pointerUp = false

                    while (!pointerUp) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull() ?: continue
                        if (change.changedToUp()) {
                            pointerUp = true
                        } else {
                            val positionChange = change.positionChange()
                            totalX += positionChange.x
                            totalY += positionChange.y
                            change.consume()
                        }
                    }

                    val absX = abs(totalX)
                    val absY = abs(totalY)
                    when {
                        absX > SWIPE_THRESHOLD && absX > absY -> {
                            if (totalX < 0) onSwipeLeft() else onSwipeRight()
                        }
                        absX < TAP_THRESHOLD && absY < TAP_THRESHOLD -> onTap()
                    }
                }
            }
    ) {
        if (matrix.width == 0 || matrix.height == 0) return@Canvas

        val cellW = size.width / matrix.width
        val cellH = size.height / matrix.height
        val cellSize = minOf(cellW, cellH)

        // Center the grid within the available canvas area.
        val totalW = cellSize * matrix.width
        val totalH = cellSize * matrix.height
        val offsetX = (size.width - totalW) / 2f
        val offsetY = (size.height - totalH) / 2f

        val gap = 0.10f * cellSize
        val dotSize = cellSize - 2 * gap
        val corner = dotSize * 0.06f
        val bezelColor = Color(0xFF3A3A3A)

        for (y in 0 until matrix.height) {
            for (x in 0 until matrix.width) {
                val color = matrix[x, y]
                if (color == Color.Black) continue

                val left = offsetX + x * cellSize + gap
                val top = offsetY + y * cellSize + gap

                // Dark gray bezel / outline that mimics the LED housing.
                val bezelSize = dotSize * 1.12f
                val bezelOffset = (bezelSize - dotSize) / 2f
                drawRoundRect(
                    color = bezelColor,
                    topLeft = Offset(left - bezelOffset, top - bezelOffset),
                    size = Size(bezelSize, bezelSize),
                    cornerRadius = CornerRadius(bezelSize * 0.06f, bezelSize * 0.06f)
                )

                // Soft glow around the pixel.
                val glowSize = dotSize * 1.22f
                val glowOffset = (glowSize - dotSize) / 2f
                drawRoundRect(
                    color = color.copy(alpha = 0.22f),
                    topLeft = Offset(left - glowOffset, top - glowOffset),
                    size = Size(glowSize, glowSize),
                    cornerRadius = CornerRadius(glowSize * 0.06f, glowSize * 0.06f)
                )

                // Main pixel: almost square with a tiny rounding.
                drawRoundRect(
                    color = color,
                    topLeft = Offset(left, top),
                    size = Size(dotSize, dotSize),
                    cornerRadius = CornerRadius(corner, corner)
                )
            }
        }
    }
}

@Composable
private fun KeepScreenOn() {
    val context = LocalContext.current
    DisposableEffect(Unit) {
        val window = (context as? android.app.Activity)?.window
        window?.let {
            it.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            WindowInsetsControllerCompat(it, it.decorView).let { controller ->
                controller.hide(WindowInsetsCompat.Type.systemBars())
                controller.systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        }
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }
}

private const val SWIPE_THRESHOLD = 120f
private const val TAP_THRESHOLD = 24f
