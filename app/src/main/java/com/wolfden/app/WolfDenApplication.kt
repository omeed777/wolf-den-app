package com.wolfden.app

import android.app.Application
import android.content.Intent
import android.os.Build

class WolfDenApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                getSharedPreferences("wolf_den_crash", MODE_PRIVATE).edit()
                    .putString("message", throwable.stackTraceToString())
                    .apply()
                val intent = Intent(this, CrashActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    putExtra("message", throwable.stackTraceToString())
                }
                startActivity(intent)
            } catch (_: Exception) {
                previous?.uncaughtException(thread, throwable)
            }
        }
    }
}
