package com.example.relojandroid.data

import com.example.relojandroid.engine.ClockArt
import kotlinx.serialization.Serializable

@Serializable
data class Settings(
    val enabledFaces: List<String> = listOf("clock", "weather", "exchange", "calendar"),
    val rotationSeconds: Int = 10,
    val weatherCity: String = "Asunción",
    val weatherLat: Double = -25.2867,
    val weatherLon: Double = -57.3333,
    val exchangeSource: String = "bcp",
    val serverPort: Int = 8080,
    val brightness: Float = 1.0f,
    val clockArt: String = ClockArt.ABSTRACT.name,
    val clockIconId: String? = null,
    val clockIconThumbnailPath: String? = null,
    val exchangeIconId: String? = null,
    val exchangeIconThumbnailPath: String? = null,
    val calendarIconId: String? = null,
    val calendarIconThumbnailPath: String? = null,
    val calendarDatePattern: String = "dd/MM"
)
