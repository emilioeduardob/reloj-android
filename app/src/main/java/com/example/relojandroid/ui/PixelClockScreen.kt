package com.example.relojandroid.ui

import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.relojandroid.engine.PixelMatrix
import kotlinx.coroutines.flow.StateFlow
import android.view.WindowManager

@Composable
fun PixelClockScreen(
    matrixFlow: StateFlow<PixelMatrix>,
    modifier: Modifier = Modifier
) {
    val matrix by matrixFlow.collectAsStateWithLifecycle()

    // Keep screen on and immersive while this screen is visible.
    KeepScreenOn()

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        PixelCanvas(matrix = matrix, modifier = Modifier.fillMaxSize())
    }
}

@Composable
private fun PixelCanvas(
    matrix: PixelMatrix,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        if (matrix.width == 0 || matrix.height == 0) return@Canvas

        val cellW = size.width / matrix.width
        val cellH = size.height / matrix.height
        val cellSize = minOf(cellW, cellH)

        // Center the grid within the available canvas area.
        val totalW = cellSize * matrix.width
        val totalH = cellSize * matrix.height
        val offsetX = (size.width - totalW) / 2f
        val offsetY = (size.height - totalH) / 2f

        val gap = 0.08f * cellSize
        val dotSize = cellSize - 2 * gap
        val corner = dotSize * 0.06f

        for (y in 0 until matrix.height) {
            for (x in 0 until matrix.width) {
                val color = matrix[x, y]
                if (color == Color.Black) continue

                val left = offsetX + x * cellSize + gap
                val top = offsetY + y * cellSize + gap

                // Soft glow / shadow behind the pixel.
                val glowSize = dotSize * 1.25f
                val glowOffset = (glowSize - dotSize) / 2f
                drawRoundRect(
                    color = color.copy(alpha = 0.28f),
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
