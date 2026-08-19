package com.example.relojandroid

import android.os.Bundle

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import com.example.relojandroid.ui.PixelClockScreen
import com.example.relojandroid.ui.theme.RelojAndroidTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val app = applicationContext as? RelojApplication
            ?: throw IllegalStateException("Application must be RelojApplication")

        setContent {
            RelojAndroidTheme {
                PixelClockScreen(
                    matrixFlow = app.faceEngine.matrix,
                    onSwipeLeft = { app.faceEngine.nextFace() },
                    onSwipeRight = { app.faceEngine.previousFace() },
                    onTap = { app.faceEngine.onTap() }
                )
            }
        }
    }
}
