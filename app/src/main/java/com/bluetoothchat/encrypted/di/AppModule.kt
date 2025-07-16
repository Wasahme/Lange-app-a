package com.bluetoothchat.encrypted.di

import android.content.Context
import com.bluetoothchat.encrypted.audio.AudioManager
import com.bluetoothchat.encrypted.bluetooth.BluetoothManager
import com.bluetoothchat.encrypted.crypto.CryptoManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for providing application-level dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideCryptoManager(): CryptoManager {
        return CryptoManager()
    }

    @Provides
    @Singleton
    fun provideBluetoothManager(
        @ApplicationContext context: Context,
        cryptoManager: CryptoManager
    ): BluetoothManager {
        return BluetoothManager(context, cryptoManager)
    }

    @Provides
    @Singleton
    fun provideAudioManager(
        cryptoManager: CryptoManager
    ): AudioManager {
        return AudioManager(cryptoManager)
    }
}