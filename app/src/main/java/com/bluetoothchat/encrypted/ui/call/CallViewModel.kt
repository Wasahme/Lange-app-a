package com.bluetoothchat.encrypted.ui.call

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bluetoothchat.encrypted.audio.AudioManager
import com.bluetoothchat.encrypted.audio.CallState
import com.bluetoothchat.encrypted.bluetooth.BluetoothManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltViewModel
class CallViewModel @Inject constructor(
    private val audioManager: AudioManager,
    private val bluetoothManager: BluetoothManager
) : ViewModel() {

    // Call state
    val callState: StateFlow<CallState> = audioManager.callState

    // Audio level
    val audioLevel: StateFlow<Float> = audioManager.audioLevel

    // Call duration
    private val _callDuration = MutableStateFlow("00:00")
    val callDuration: StateFlow<String> = _callDuration.asStateFlow()

    // Mute state
    private val _isMuted = MutableStateFlow(false)
    val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()

    // Speaker state
    private val _isSpeakerOn = MutableStateFlow(false)
    val isSpeakerOn: StateFlow<Boolean> = _isSpeakerOn.asStateFlow()

    // Connected device
    private val _connectedDeviceName = MutableStateFlow("")
    val connectedDeviceName: StateFlow<String> = _connectedDeviceName.asStateFlow()

    // Error messages
    private val _errorMessage = MutableStateFlow("")
    val errorMessage: StateFlow<String> = _errorMessage.asStateFlow()

    // Call start time
    private var callStartTime: Long = 0
    private var durationUpdateJob: kotlinx.coroutines.Job? = null

    init {
        observeBluetoothState()
        observeCallState()
        observeAudioStream()
    }

    /**
     * Start voice call
     */
    fun startCall() {
        viewModelScope.launch {
            try {
                if (audioManager.initialize()) {
                    if (audioManager.startCall()) {
                        callStartTime = System.currentTimeMillis()
                        startDurationTimer()
                        Timber.d("Voice call started")
                    } else {
                        _errorMessage.value = "Failed to start voice call"
                    }
                } else {
                    _errorMessage.value = "Failed to initialize audio system"
                }
            } catch (e: Exception) {
                Timber.e(e, "Error starting call")
                _errorMessage.value = "Error starting call: ${e.message}"
            }
        }
    }

    /**
     * End voice call
     */
    fun endCall() {
        viewModelScope.launch {
            try {
                audioManager.endCall()
                stopDurationTimer()
                Timber.d("Voice call ended")
            } catch (e: Exception) {
                Timber.e(e, "Error ending call")
                _errorMessage.value = "Error ending call: ${e.message}"
            }
        }
    }

    /**
     * Toggle microphone mute
     */
    fun toggleMute() {
        viewModelScope.launch {
            try {
                val newMuteState = !_isMuted.value
                audioManager.setMicrophoneMuted(newMuteState)
                _isMuted.value = newMuteState
                Timber.d("Microphone muted: $newMuteState")
            } catch (e: Exception) {
                Timber.e(e, "Error toggling mute")
                _errorMessage.value = "Error toggling mute: ${e.message}"
            }
        }
    }

    /**
     * Toggle speaker
     */
    fun toggleSpeaker() {
        viewModelScope.launch {
            try {
                val newSpeakerState = !_isSpeakerOn.value
                _isSpeakerOn.value = newSpeakerState
                
                // Adjust volume based on speaker state
                val volume = if (newSpeakerState) 1.0f else 0.7f
                audioManager.setSpeakerVolume(volume)
                
                Timber.d("Speaker enabled: $newSpeakerState")
            } catch (e: Exception) {
                Timber.e(e, "Error toggling speaker")
                _errorMessage.value = "Error toggling speaker: ${e.message}"
            }
        }
    }

    /**
     * Set volume level
     */
    fun setVolume(volume: Float) {
        viewModelScope.launch {
            try {
                audioManager.setSpeakerVolume(volume)
                Timber.d("Volume set to: $volume")
            } catch (e: Exception) {
                Timber.e(e, "Error setting volume")
                _errorMessage.value = "Error setting volume: ${e.message}"
            }
        }
    }

    /**
     * Observe Bluetooth connection state
     */
    private fun observeBluetoothState() {
        viewModelScope.launch {
            bluetoothManager.connectedDevice.collect { device ->
                _connectedDeviceName.value = device?.name ?: "Unknown Device"
            }
        }
    }

    /**
     * Observe call state changes
     */
    private fun observeCallState() {
        viewModelScope.launch {
            callState.collect { state ->
                when (state) {
                    CallState.ACTIVE -> {
                        if (callStartTime == 0L) {
                            callStartTime = System.currentTimeMillis()
                            startDurationTimer()
                        }
                    }
                    CallState.IDLE, CallState.ERROR -> {
                        stopDurationTimer()
                        _callDuration.value = "00:00"
                    }
                    else -> {
                        // Handle other states if needed
                    }
                }
            }
        }
    }

    /**
     * Observe audio stream for transmission
     */
    private fun observeAudioStream() {
        viewModelScope.launch {
            audioManager.getOutgoingAudioStream().collect { audioData ->
                try {
                    // Send audio data via Bluetooth
                    bluetoothManager.sendMessage(
                        audioData,
                        com.bluetoothchat.encrypted.crypto.MessageType.VOICE_DATA
                    )
                } catch (e: Exception) {
                    Timber.e(e, "Error sending audio data")
                }
            }
        }

        // Observe incoming audio
        viewModelScope.launch {
            bluetoothManager.receiveMessages().collect { encryptedMessage ->
                if (encryptedMessage.messageType == com.bluetoothchat.encrypted.crypto.MessageType.VOICE_DATA) {
                    try {
                        // Decrypt and play audio
                        val decryptedAudio = com.bluetoothchat.encrypted.crypto.CryptoManager().decryptMessage(encryptedMessage)
                        audioManager.sendIncomingAudio(decryptedAudio)
                    } catch (e: Exception) {
                        Timber.e(e, "Error processing incoming audio")
                    }
                }
            }
        }
    }

    /**
     * Start call duration timer
     */
    private fun startDurationTimer() {
        durationUpdateJob = viewModelScope.launch {
            while (true) {
                val currentTime = System.currentTimeMillis()
                val duration = currentTime - callStartTime
                _callDuration.value = formatDuration(duration)
                kotlinx.coroutines.delay(1000) // Update every second
            }
        }
    }

    /**
     * Stop call duration timer
     */
    private fun stopDurationTimer() {
        durationUpdateJob?.cancel()
        durationUpdateJob = null
        callStartTime = 0
    }

    /**
     * Format duration in MM:SS format
     */
    private fun formatDuration(durationMs: Long): String {
        val minutes = TimeUnit.MILLISECONDS.toMinutes(durationMs)
        val seconds = TimeUnit.MILLISECONDS.toSeconds(durationMs) % 60
        return String.format("%02d:%02d", minutes, seconds)
    }

    /**
     * Get current call duration in milliseconds
     */
    fun getCallDurationMs(): Long {
        return if (callStartTime > 0) {
            System.currentTimeMillis() - callStartTime
        } else {
            0
        }
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _errorMessage.value = ""
    }

    /**
     * Cleanup resources
     */
    fun cleanup() {
        stopDurationTimer()
        audioManager.cleanup()
    }

    override fun onCleared() {
        super.onCleared()
        cleanup()
    }
}