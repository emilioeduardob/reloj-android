package com.example.relojandroid

import com.example.relojandroid.faces.ClockFace
import com.example.relojandroid.faces.ExchangeFace
import com.example.relojandroid.faces.WeatherFace
import com.example.relojandroid.engine.Face

object AppModule {
    fun provideFaces(): List<Face> = listOf(
        ClockFace(),
        WeatherFace(),
        ExchangeFace()
    )
}
