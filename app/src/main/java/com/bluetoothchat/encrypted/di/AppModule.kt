package com.bluetoothchat.encrypted.di

import android.content.Context
import com.bluetoothchat.encrypted.ai.AIManager
import com.bluetoothchat.encrypted.audio.AdvancedAudioManager
import com.bluetoothchat.encrypted.bluetooth.AdvancedBluetoothManager
import com.bluetoothchat.encrypted.crypto.AdvancedCryptoManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideAdvancedCryptoManager(): AdvancedCryptoManager {
        return AdvancedCryptoManager()
    }

    @Provides
    @Singleton
    fun provideAdvancedAudioManager(
        cryptoManager: AdvancedCryptoManager
    ): AdvancedAudioManager {
        return AdvancedAudioManager(cryptoManager)
    }

    @Provides
    @Singleton
    fun provideAdvancedBluetoothManager(
        @ApplicationContext context: Context,
        cryptoManager: AdvancedCryptoManager
    ): AdvancedBluetoothManager {
        return AdvancedBluetoothManager(context, cryptoManager)
    }

    @Provides
    @Singleton
    fun provideAIManager(
        @ApplicationContext context: Context
    ): AIManager {
        return AIManager(context)
    }
}