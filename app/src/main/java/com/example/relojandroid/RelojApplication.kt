package com.example.relojandroid

import android.app.Application
import com.example.relojandroid.data.SettingsRepository
import com.example.relojandroid.engine.FaceEngine
import com.example.relojandroid.server.WebServer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class RelojApplication : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    lateinit var settingsRepository: SettingsRepository
        private set
    lateinit var faceEngine: FaceEngine
        private set
    lateinit var webServer: WebServer
        private set

    override fun onCreate() {
        super.onCreate()

        settingsRepository = SettingsRepository(this)
        val faces = AppModule.provideFaces()
        faceEngine = FaceEngine(faces, settingsRepository.settings)
        webServer = WebServer(this, settingsRepository, faces, faceEngine)

        applicationScope.launch {
            val settings = settingsRepository.settings.first()
            webServer.start(settings.serverPort)
        }

        applicationScope.launch {
            faceEngine.run()
        }
    }
}
