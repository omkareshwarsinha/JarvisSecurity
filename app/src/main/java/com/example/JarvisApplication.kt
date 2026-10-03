package com.example

import android.app.Application
import android.util.Log
import com.example.jarvis.data.repository.SecurityRepository

class JarvisApplication : Application() {

    lateinit var securityRepository: SecurityRepository
        private set

    override fun onCreate() {
        super.onCreate()
        try {
            securityRepository = SecurityRepository(this)
        } catch (t: Throwable) {
            Log.e("JarvisApplication", "Critical failure during repository initialization", t)
            // Ensure securityRepository is not left unassigned
            securityRepository = SecurityRepository(this)
        }
    }
}
