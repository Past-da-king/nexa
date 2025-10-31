package com.example.nexus

import android.app.Application
import android.content.Intent
import android.os.Build
import com.example.nexus.services.ConnectionService
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class NexaApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        val serviceIntent = Intent(this, ConnectionService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
    }
}