package com.example.relojandroid

import android.app.Application
import android.util.Log
import com.example.relojandroid.data.SettingsRepository
import com.example.relojandroid.engine.FaceEngine
import com.example.relojandroid.server.WebServer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
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
        val iconRepository = AppModule.provideIconRepository(this)
        val faces = AppModule.provideFaces(iconRepository)
        faceEngine = FaceEngine(faces, settingsRepository.settings, applicationScope)
        webServer = WebServer(this, settingsRepository, iconRepository, faces, faceEngine)

        applicationScope.launch {
            try {
                faceEngine.run()
            } catch (e: Exception) {
                Log.e(TAG, "Face engine crashed", e)
            }
        }

        // Restart the web server whenever the configured port changes.
        settingsRepository.settings
            .map { it.serverPort }
            .distinctUntilChanged()
            .onEach { port ->
                try {
                    webServer.start(port)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to start web server on port $port", e)
                }
            }
            .launchIn(applicationScope)
    }

    companion object {
        private const val TAG = "RelojApplication"
    }
}
