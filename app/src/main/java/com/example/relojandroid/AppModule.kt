package com.example.relojandroid

import android.content.Context
import com.example.relojandroid.data.IconRepository
import com.example.relojandroid.data.LaMetricIconApi
import com.example.relojandroid.faces.CalendarFace
import com.example.relojandroid.faces.ClockFace
import com.example.relojandroid.faces.ExchangeFace
import com.example.relojandroid.faces.WeatherFace
import com.example.relojandroid.engine.Face

object AppModule {

    fun provideIconRepository(context: Context): IconRepository {
        return IconRepository(context, LaMetricIconApi())
    }

    fun provideFaces(iconRepository: IconRepository): List<Face> = listOf(
        ClockFace(iconRepository),
        WeatherFace(),
        ExchangeFace(iconRepository = iconRepository),
        CalendarFace(iconRepository)
    )
}
