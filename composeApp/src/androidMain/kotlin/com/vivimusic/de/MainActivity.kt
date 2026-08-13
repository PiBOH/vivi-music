package com.vivimusic.de

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.vivimusic.de.data.AppConfig
import com.vivimusic.de.data.AppContainer
import com.vivimusic.de.data.AppContextHolder
import com.vivimusic.de.ui.App
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        AppContextHolder.context = applicationContext
        AppConfig.supabaseUrl = BuildConfig.SUPABASE_URL
        AppConfig.supabaseAnonKey = BuildConfig.SUPABASE_ANON_KEY

        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val container = AppContainer(scope)
        container.start()

        setContent {
            App(container)
        }
    }
}
