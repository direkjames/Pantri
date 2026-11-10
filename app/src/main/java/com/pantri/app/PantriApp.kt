package com.pantri.app

import android.app.Application
import android.content.pm.ApplicationInfo
import android.os.StrictMode
import com.pantri.app.data.BackupService
import com.pantri.app.data.PantriDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import com.pantri.app.data.PantriRepository
import com.pantri.app.data.SettingsStore

class PantriApp : Application() {
    lateinit var repo: PantriRepository
        private set
    lateinit var backup: BackupService
        private set

    override fun onCreate() {
        super.onCreate()
        // Debug builds only: log any disk or network work on the main thread and any leaked resources.
        if (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0) {
            StrictMode.setThreadPolicy(StrictMode.ThreadPolicy.Builder().detectAll().penaltyLog().build())
            StrictMode.setVmPolicy(StrictMode.VmPolicy.Builder().detectAll().penaltyLog().build())
        }
        repo = PantriRepository(PantriDatabase.create(this), SettingsStore(this))
        backup = BackupService(this, repo)
    }

    /** Outlives any screen, so an automatic backup is never cut off by closing the app. */
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
}