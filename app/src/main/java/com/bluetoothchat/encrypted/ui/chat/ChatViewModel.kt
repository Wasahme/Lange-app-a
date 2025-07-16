package com.bluetoothchat.encrypted.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bluetoothchat.encrypted.bluetooth.BluetoothManager
import com.bluetoothchat.encrypted.bluetooth.ConnectionState
import com.bluetoothchat.encrypted.crypto.CryptoManager
import com.bluetoothchat.encrypted.crypto.MessageType
import com.bluetoothchat.encrypted.data.model.Message
import com.bluetoothchat.encrypted.data.model.MessageStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.*
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val bluetoothManager: BluetoothManager,
    private val cryptoManager: CryptoManager
) : ViewModel() {

    // Messages
    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    // Connection status
    private val _connectionStatus = MutableStateFlow("Disconnected")
    val connectionStatus: StateFlow<String> = _connectionStatus.asStateFlow()

    // Encryption status
    private val _encryptionStatus = MutableStateFlow(false)
    val encryptionStatus: StateFlow<Boolean> = _encryptionStatus.asStateFlow()

    // Connected device name
    private val _connectedDeviceName = MutableStateFlow("")
    val connectedDeviceName: StateFlow<String> = _connectedDeviceName.asStateFlow()

    // Typing indicator
    private val _isTyping = MutableStateFlow(false)
    val isTyping: StateFlow<Boolean> = _isTyping.asStateFlow()

    // Error messages
    private val _errorMessage = MutableStateFlow("")
    val errorMessage: StateFlow<String> = _errorMessage.asStateFlow()

    // Message sending state
    private val _isSendingMessage = MutableStateFlow(false)
    val isSendingMessage: StateFlow<Boolean> = _isSendingMessage.asStateFlow()

    init {
        observeBluetoothState()
        observeIncomingMessages()
    }

    /**
     * Initialize chat functionality
     */
    fun initializeChat() {
        viewModelScope.launch {
            try {
                // Check connection state
                val connectionState = bluetoothManager.connectionState.value
                updateConnectionStatus(connectionState)
                
                // Check if we have a connected device
                bluetoothManager.connectedDevice.value?.let { device ->
                    _connectedDeviceName.value = device.name ?: "Unknown Device"
                }
                
                Timber.d("Chat initialized")
            } catch (e: Exception) {
                Timber.e(e, "Failed to initialize chat")
                _errorMessage.value = "Failed to initialize chat: ${e.message}"
            }
        }
    }

    /**
     * Send a text message
     */
    fun sendMessage(text: String) {
        viewModelScope.launch {
            _isSendingMessage.value = true
            
            try {
                // Create message
                val message = Message(
                    id = UUID.randomUUID().toString(),
                    text = text,
                    timestamp = System.currentTimeMillis(),
                    isFromMe = true,
                    status = MessageStatus.SENDING,
                    isEncrypted = _encryptionStatus.value
                )
                
                // Add to messages list
                addMessage(message)
                
                // Send via Bluetooth
                val success = bluetoothManager.sendMessage(
                    text.toByteArray(),
                    MessageType.TEXT_MESSAGE
                )
                
                // Update message status
                if (success) {
                    updateMessageStatus(message.id, MessageStatus.SENT)
                    Timber.d("Message sent successfully")
                } else {
                    updateMessageStatus(message.id, MessageStatus.FAILED)
                    _errorMessage.value = "Failed to send message"
                }
                
            } catch (e: Exception) {
                Timber.e(e, "Error sending message")
                _errorMessage.value = "Error sending message: ${e.message}"
            } finally {
                _isSendingMessage.value = false
            }
        }
    }

    /**
     * Observe Bluetooth connection state
     */
    private fun observeBluetoothState() {
        viewModelScope.launch {
            bluetoothManager.connectionState.collect { state ->
                updateConnectionStatus(state)
            }
        }
        
        viewModelScope.launch {
            bluetoothManager.connectedDevice.collect { device ->
                _connectedDeviceName.value = device?.name ?: ""
                
                // Update encryption status based on connection
                _encryptionStatus.value = device != null && 
                    bluetoothManager.connectionState.value == ConnectionState.CONNECTED
            }
        }
    }

    /**
     * Observe incoming messages
     */
    private fun observeIncomingMessages() {
        viewModelScope.launch {
            bluetoothManager.receiveMessages().collect { encryptedMessage ->
                try {
                    when (encryptedMessage.messageType) {
                        MessageType.TEXT_MESSAGE -> {
                            handleIncomingTextMessage(encryptedMessage)
                        }
                        MessageType.PING -> {
                            // Handle ping messages
                            Timber.d("Received ping message")
                        }
                        MessageType.ACK -> {
                            // Handle acknowledgment messages
                            Timber.d("Received ACK message")
                        }
                        else -> {
                            Timber.d("Received message of type: ${encryptedMessage.messageType}")
                        }
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Error processing incoming message")
                    _errorMessage.value = "Error processing incoming message"
                }
            }
        }
    }

    /**
     * Handle incoming text message
     */
    private suspend fun handleIncomingTextMessage(encryptedMessage: com.bluetoothchat.encrypted.crypto.EncryptedMessage) {
        try {
            // Decrypt message
            val decryptedBytes = cryptoManager.decryptMessage(encryptedMessage)
            val messageText = String(decryptedBytes)
            
            // Create message object
            val message = Message(
                id = UUID.randomUUID().toString(),
                text = messageText,
                timestamp = System.currentTimeMillis(),
                isFromMe = false,
                status = MessageStatus.RECEIVED,
                isEncrypted = true
            )
            
            // Add to messages list
            addMessage(message)
            
            Timber.d("Received encrypted message: $messageText")
            
        } catch (e: Exception) {
            Timber.e(e, "Failed to decrypt incoming message")
            _errorMessage.value = "Failed to decrypt incoming message"
        }
    }

    /**
     * Add message to the list
     */
    private fun addMessage(message: Message) {
        val currentMessages = _messages.value.toMutableList()
        currentMessages.add(message)
        _messages.value = currentMessages
    }

    /**
     * Update message status
     */
    private fun updateMessageStatus(messageId: String, status: MessageStatus) {
        val currentMessages = _messages.value.toMutableList()
        val messageIndex = currentMessages.indexOfFirst { it.id == messageId }
        
        if (messageIndex != -1) {
            currentMessages[messageIndex] = currentMessages[messageIndex].copy(status = status)
            _messages.value = currentMessages
        }
    }

    /**
     * Update connection status
     */
    private fun updateConnectionStatus(state: ConnectionState) {
        _connectionStatus.value = when (state) {
            ConnectionState.CONNECTED -> "Connected"
            ConnectionState.CONNECTING -> "Connecting..."
            ConnectionState.DISCONNECTED -> "Disconnected"
            ConnectionState.LISTENING -> "Listening"
            ConnectionState.ERROR -> "Connection Error"
        }
        
        // Update encryption status
        _encryptionStatus.value = state == ConnectionState.CONNECTED
    }

    /**
     * Clear all messages
     */
    fun clearMessages() {
        _messages.value = emptyList()
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _errorMessage.value = ""
    }

    /**
     * Simulate typing indicator (for future implementation)
     */
    fun startTyping() {
        _isTyping.value = true
        
        // Auto-stop typing after 3 seconds
        viewModelScope.launch {
            kotlinx.coroutines.delay(3000)
            _isTyping.value = false
        }
    }

    /**
     * Get message count
     */
    fun getMessageCount(): Int {
        return _messages.value.size
    }

    /**
     * Get last message
     */
    fun getLastMessage(): Message? {
        return _messages.value.lastOrNull()
    }

    /**
     * Cleanup resources
     */
    fun cleanup() {
        // Clear sensitive data
        _messages.value = emptyList()
        _errorMessage.value = ""
    }

    override fun onCleared() {
        super.onCleared()
        cleanup()
    }
}