package com.example.relojandroid.engine

import com.example.relojandroid.data.Settings

interface Face {
    val id: String
    val name: String
    suspend fun render(settings: Settings): PixelMatrix
    suspend fun isAvailable(settings: Settings): Boolean = true

    /**
     * Return true when this face produces animated output and should be rendered
     * more frequently than the default 200 ms cadence.
     */
    suspend fun isAnimated(settings: Settings): Boolean = false

    /**
     * Called when the user taps the screen while this face is active.
     * Faces can use this to refresh their content or toggle a state.
     */
    suspend fun onTap(settings: Settings) {}
}
