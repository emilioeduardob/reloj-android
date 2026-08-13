package com.example.relojandroid.engine

import com.example.relojandroid.data.Settings

interface Face {
    val id: String
    val name: String
    suspend fun render(settings: Settings): PixelMatrix
    suspend fun isAvailable(settings: Settings): Boolean = true
}
