package com.pantri.app

import android.app.Application
import com.pantri.app.data.PantriDatabase
import com.pantri.app.data.PantriRepository
import com.pantri.app.data.SettingsStore

class PantriApp : Application() {
    lateinit var repo: PantriRepository
        private set

    override fun onCreate() {
        super.onCreate()
        repo = PantriRepository(PantriDatabase.create(this), SettingsStore(this))
    }
}
