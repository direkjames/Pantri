package com.pantri.app

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.pantri.app.data.Settings
import com.pantri.app.ui.PantriRoot
import com.pantri.app.ui.theme.PantriTheme
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    // Each time the app comes to the front, take the scheduled backup if one is due.
    override fun onStart() {
        super.onStart()
        val app = application as PantriApp
        app.appScope.launch { app.backup.autoBackupIfDue() }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        val repo = (application as PantriApp).repo
        // Hold the splash until settings load, so a dark-mode user never sees a light flash.
        val settings = repo.settingsFlow.stateIn(lifecycleScope, SharingStarted.Eagerly, null)
        splash.setKeepOnScreenCondition { settings.value == null }
        setContent {
            val s by settings.collectAsStateWithLifecycle()
            val current = s ?: Settings()
            DisposableEffect(current.darkMode) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { current.darkMode },
                    navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { current.darkMode },
                )
                onDispose { }
            }
            PantriTheme(current.darkMode) { PantriRoot(current.haptics) }
        }
    }
}
