package com.vivimusic.de.data

import com.vivimusic.de.data.db.AppDatabase
import com.vivimusic.de.data.db.getRoomDatabase
import com.vivimusic.de.data.network.InnerTubeClient
import com.vivimusic.de.data.network.createHttpClient
import com.vivimusic.de.data.playback.AudioEngine
import com.vivimusic.de.data.playback.createAudioEngine
import com.vivimusic.de.data.sync.SupabaseSyncClient
import com.vivimusic.de.data.sync.SyncManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Manual dependency container for the application. Created once by each
 * platform entry point after [AppConfig] has been populated.
 */
class AppContainer(val scope: CoroutineScope) {
    val database: AppDatabase = getRoomDatabase()

    private val httpClient = createHttpClient()

    val innerTube: InnerTubeClient = InnerTubeClient(httpClient, AppConfig.innerTubeApiKey)

    private val syncClient: SupabaseSyncClient? =
        if (AppConfig.isSyncConfigured) SupabaseSyncClient.create() else null

    val syncManager: SyncManager = SyncManager(database, syncClient, scope)

    val repository: MusicRepository = MusicRepository(database, innerTube, syncManager, scope)

    val audioEngine: AudioEngine = createAudioEngine()

    fun start() {
        if (syncManager.isEnabled) {
            syncManager.startRealtime()
            scope.launch {
                syncManager.fullSync()
            }
        }
    }
}
