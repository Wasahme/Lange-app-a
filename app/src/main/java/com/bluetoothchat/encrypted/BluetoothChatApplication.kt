package com.bluetoothchat.encrypted

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

/**
 * Main application class for the encrypted Bluetooth chat application
 * Initializes dependency injection and core services
 */
@HiltAndroidApp
class BluetoothChatApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        
        // Initialize logging
        initializeLogging()
        
        Timber.d("BluetoothChatApplication initialized")
    }

    /**
     * Initialize logging system
     */
    private fun initializeLogging() {
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        } else {
            // In production, use a custom tree that doesn't log debug messages
            Timber.plant(ReleaseTree())
        }
    }

    /**
     * Custom Timber tree for release builds
     */
    private class ReleaseTree : Timber.Tree() {
        override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
            // Only log warnings and errors in release builds
            if (priority >= android.util.Log.WARN) {
                // In production, send to crash reporting service
                // For now, just use system logging
                when (priority) {
                    android.util.Log.WARN -> android.util.Log.w(tag, message, t)
                    android.util.Log.ERROR -> android.util.Log.e(tag, message, t)
                }
            }
        }
    }
}