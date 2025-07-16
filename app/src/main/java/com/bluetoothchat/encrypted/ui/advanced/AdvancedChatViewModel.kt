package com.bluetoothchat.encrypted.ui.advanced

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bluetoothchat.encrypted.ai.AIManager
import com.bluetoothchat.encrypted.audio.AdvancedAudioManager
import com.bluetoothchat.encrypted.bluetooth.AdvancedBluetoothManager
import com.bluetoothchat.encrypted.crypto.AdvancedCryptoManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.*
import javax.inject.Inject

@HiltViewModel
class AdvancedChatViewModel @Inject constructor(
    private val bluetoothManager: AdvancedBluetoothManager,
    private val cryptoManager: AdvancedCryptoManager,
    private val audioManager: AdvancedAudioManager,
    private val aiManager: AIManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdvancedChatUiState())
    val uiState: StateFlow<AdvancedChatUiState> = _uiState.asStateFlow()

    private var currentDeviceAddress: String = ""

    fun initializeChat(deviceAddress: String) {
        currentDeviceAddress = deviceAddress
        
        // Initialize connection monitoring
        viewModelScope.launch {
            bluetoothManager.connectionState.collect { state ->
                _uiState.value = _uiState.value.copy(
                    isConnected = state == com.bluetoothchat.encrypted.bluetooth.BluetoothState.CONNECTED
                )
            }
        }
        
        // Add some demo messages
        addDemoMessages()
    }

    private fun addDemoMessages() {
        val demoMessages = listOf(
            ChatMessage(
                id = "1",
                content = "مرحباً! كيف حالك؟",
                timestamp = System.currentTimeMillis() - 300000,
                isFromMe = false,
                status = MessageStatus.RECEIVED
            ),
            ChatMessage(
                id = "2", 
                content = "أهلاً وسهلاً! الحمد لله بخير، وأنت؟",
                timestamp = System.currentTimeMillis() - 250000,
                isFromMe = true,
                status = MessageStatus.DELIVERED
            ),
            ChatMessage(
                id = "3",
                content = "الحمد لله أنا بخير أيضاً. هل جربت الميزات الجديدة في التطبيق؟",
                timestamp = System.currentTimeMillis() - 200000,
                isFromMe = false,
                status = MessageStatus.RECEIVED
            ),
            ChatMessage(
                id = "4",
                content = "نعم! التشفير المتقدم والصوت عالي الجودة رائعان 👍",
                timestamp = System.currentTimeMillis() - 150000,
                isFromMe = true,
                status = MessageStatus.DELIVERED
            )
        )
        
        _uiState.value = _uiState.value.copy(
            messages = demoMessages,
            deviceName = "جهاز الاختبار"
        )
    }

    fun sendMessage(content: String) {
        if (content.isBlank()) return
        
        val message = ChatMessage(
            id = UUID.randomUUID().toString(),
            content = content,
            timestamp = System.currentTimeMillis(),
            isFromMe = true,
            status = MessageStatus.SENDING
        )
        
        // Add message to UI immediately
        val currentMessages = _uiState.value.messages.toMutableList()
        currentMessages.add(message)
        _uiState.value = _uiState.value.copy(messages = currentMessages)
        
        // Send message through Bluetooth
        viewModelScope.launch {
            try {
                // Analyze message with AI
                val analysis = aiManager.analyzeMessage(
                    message = content,
                    senderDevice = "self",
                    timestamp = System.currentTimeMillis()
                )
                
                // Encrypt and send message
                // This would be implemented with actual Bluetooth sending
                // For now, simulate sending
                simulateMessageSending(message)
                
                Timber.d("Message sent: $content")
                Timber.d("AI Analysis: ${analysis.sentiment}")
                
            } catch (e: Exception) {
                Timber.e(e, "Failed to send message")
                updateMessageStatus(message.id, MessageStatus.FAILED)
            }
        }
    }

    private suspend fun simulateMessageSending(message: ChatMessage) {
        kotlinx.coroutines.delay(1000) // Simulate network delay
        updateMessageStatus(message.id, MessageStatus.SENT)
        
        kotlinx.coroutines.delay(2000) // Simulate delivery delay
        updateMessageStatus(message.id, MessageStatus.DELIVERED)
    }

    private fun updateMessageStatus(messageId: String, status: MessageStatus) {
        val currentMessages = _uiState.value.messages.toMutableList()
        val messageIndex = currentMessages.indexOfFirst { it.id == messageId }
        
        if (messageIndex >= 0) {
            currentMessages[messageIndex] = currentMessages[messageIndex].copy(status = status)
            _uiState.value = _uiState.value.copy(messages = currentMessages)
        }
    }

    fun startVoiceCall() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isInCall = true)
                
                // Start audio recording and playback
                audioManager.startRecording().collect { result ->
                    when (result) {
                        is com.bluetoothchat.encrypted.audio.AudioRecordingResult.Started -> {
                            Timber.d("Voice call started")
                        }
                        is com.bluetoothchat.encrypted.audio.AudioRecordingResult.DataAvailable -> {
                            // Process audio data
                            processAudioData(result.frame.data)
                        }
                        is com.bluetoothchat.encrypted.audio.AudioRecordingResult.Error -> {
                            Timber.e("Audio recording error: ${result.message}")
                            _uiState.value = _uiState.value.copy(isInCall = false)
                        }
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to start voice call")
                _uiState.value = _uiState.value.copy(isInCall = false)
            }
        }
    }

    private suspend fun processAudioData(audioData: ByteArray) {
        // Analyze voice emotion
        val emotionAnalysis = aiManager.analyzeVoiceEmotion(audioData, 48000)
        
        // Log emotion analysis
        emotionAnalysis.dominantEmotion?.let { emotion ->
            Timber.d("Detected emotion: ${emotion.type} (confidence: ${emotion.confidence})")
        }
        
        // Encrypt and send audio data
        // This would be implemented with actual Bluetooth transmission
    }

    fun startVoiceRecording() {
        if (_uiState.value.isRecording) {
            stopVoiceRecording()
        } else {
            _uiState.value = _uiState.value.copy(isRecording = true)
            
            viewModelScope.launch {
                try {
                    audioManager.startRecording().collect { result ->
                        when (result) {
                            is com.bluetoothchat.encrypted.audio.AudioRecordingResult.Started -> {
                                Timber.d("Voice recording started")
                            }
                            is com.bluetoothchat.encrypted.audio.AudioRecordingResult.DataAvailable -> {
                                // Process recorded audio
                                processRecordedAudio(result.frame.data)
                            }
                            is com.bluetoothchat.encrypted.audio.AudioRecordingResult.Error -> {
                                Timber.e("Recording error: ${result.message}")
                                _uiState.value = _uiState.value.copy(isRecording = false)
                            }
                        }
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Failed to start recording")
                    _uiState.value = _uiState.value.copy(isRecording = false)
                }
            }
        }
    }

    private fun stopVoiceRecording() {
        _uiState.value = _uiState.value.copy(isRecording = false)
        audioManager.stopRecording()
        
        // Create voice message
        val voiceMessage = ChatMessage(
            id = UUID.randomUUID().toString(),
            content = "🎤 رسالة صوتية",
            timestamp = System.currentTimeMillis(),
            isFromMe = true,
            status = MessageStatus.SENDING
        )
        
        val currentMessages = _uiState.value.messages.toMutableList()
        currentMessages.add(voiceMessage)
        _uiState.value = _uiState.value.copy(messages = currentMessages)
        
        // Simulate sending voice message
        viewModelScope.launch {
            simulateMessageSending(voiceMessage)
        }
    }

    private suspend fun processRecordedAudio(audioData: ByteArray) {
        // Analyze audio with AI
        val emotionAnalysis = aiManager.analyzeVoiceEmotion(audioData, 48000)
        
        // Log analysis results
        emotionAnalysis.dominantEmotion?.let { emotion ->
            Timber.d("Voice emotion: ${emotion.type} (${emotion.confidence})")
        }
    }

    fun endVoiceCall() {
        _uiState.value = _uiState.value.copy(isInCall = false)
        audioManager.stopRecording()
        audioManager.stopPlayback()
    }

    override fun onCleared() {
        super.onCleared()
        audioManager.cleanup()
    }
}

data class AdvancedChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val deviceName: String? = null,
    val isConnected: Boolean = false,
    val isRecording: Boolean = false,
    val isInCall: Boolean = false,
    val error: String? = null
)