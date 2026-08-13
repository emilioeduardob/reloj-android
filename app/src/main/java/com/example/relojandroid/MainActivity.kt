package com.example.relojandroid

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.relojandroid.ui.PixelClockScreen
import com.example.relojandroid.ui.theme.RelojAndroidTheme

class MainActivity : ComponentActivity() {

    private lateinit var application: RelojApplication

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        application = this.applicationContext as RelojApplication

        enableEdgeToEdge()

        setContent {
            RelojAndroidTheme {
                PixelClockScreen(matrixFlow = application.faceEngine.matrix)
            }
        }
    }
}
