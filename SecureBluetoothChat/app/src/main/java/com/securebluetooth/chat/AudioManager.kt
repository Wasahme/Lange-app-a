package com.securebluetooth.chat

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder

class AudioManager {
    private val sampleRate = 44100
    private val bufferSize = AudioRecord.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
    private val audioRecord = AudioRecord(MediaRecorder.AudioSource.MIC, sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize)
    private val audioTrack = AudioTrack.Builder()
        .setAudioFormat(
            AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(sampleRate)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .build()
        )
        .setBufferSizeInBytes(bufferSize)
        .build()

    fun startRecording() {
        audioRecord.startRecording()
    }

    fun stopRecording() {
        audioRecord.stop()
    }

    fun playAudio(audioData: ByteArray) {
        audioTrack.play()
        audioTrack.write(audioData, 0, audioData.size)
    }

    fun compressAudio(rawData: ByteArray): ByteArray {
        // Placeholder for Opus compression
        return rawData
    }
}