package com.bluetoothchat.encrypted.bluetooth

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import com.bluetoothchat.encrypted.crypto.CryptoManager
import com.bluetoothchat.encrypted.crypto.EncryptedMessage
import com.bluetoothchat.encrypted.crypto.MessageType
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Bluetooth manager for secure peer-to-peer communication
 * Features:
 * - Device discovery and pairing
 * - Secure connection establishment
 * - Encrypted data transmission
 * - Connection state management
 * - Auto-reconnection capabilities
 */
@Singleton
class BluetoothManager @Inject constructor(
    private val context: Context,
    private val cryptoManager: CryptoManager
) {

    companion object {
        private const val APP_NAME = "BluetoothChat"
        private val APP_UUID = UUID.fromString("8ce255c0-200a-11e0-ac64-0800200c9a66")
        private const val BUFFER_SIZE = 1024
        private const val VOICE_BUFFER_SIZE = 4096
        private const val CONNECTION_TIMEOUT = 30000L // 30 seconds
        private const val RECONNECT_DELAY = 5000L // 5 seconds
        private const val MAX_RECONNECT_ATTEMPTS = 3
    }

    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    // Connection state
    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()
    
    // Discovered devices
    private val _discoveredDevices = MutableStateFlow<List<BluetoothDevice>>(emptyList())
    val discoveredDevices: StateFlow<List<BluetoothDevice>> = _discoveredDevices.asStateFlow()
    
    // Connected device
    private val _connectedDevice = MutableStateFlow<BluetoothDevice?>(null)
    val connectedDevice: StateFlow<BluetoothDevice?> = _connectedDevice.asStateFlow()
    
    // Message channels
    private val incomingMessages = Channel<EncryptedMessage>(Channel.UNLIMITED)
    private val outgoingMessages = Channel<EncryptedMessage>(Channel.UNLIMITED)
    
    // Connection objects
    private var serverSocket: BluetoothServerSocket? = null
    private var clientSocket: BluetoothSocket? = null
    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null
    
    // Connection management
    private var connectionJob: Job? = null
    private var messageHandlingJob: Job? = null
    private var reconnectAttempts = 0
    private var isServer = false

    /**
     * Initialize Bluetooth manager
     */
    fun initialize(): Boolean {
        return bluetoothAdapter?.isEnabled == true
    }

    /**
     * Check if Bluetooth is available and enabled
     */
    fun isBluetoothAvailable(): Boolean {
        return bluetoothAdapter != null && bluetoothAdapter.isEnabled
    }

    /**
     * Check if we have necessary permissions
     */
    fun hasPermissions(): Boolean {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED
        } else {
            ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_ADMIN) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * Start device discovery
     */
    fun startDiscovery(): Boolean {
        if (!hasPermissions() || bluetoothAdapter == null) {
            Timber.e("Bluetooth permissions not granted or adapter not available")
            return false
        }

        return try {
            if (bluetoothAdapter.isDiscovering) {
                bluetoothAdapter.cancelDiscovery()
            }
            
            _discoveredDevices.value = emptyList()
            bluetoothAdapter.startDiscovery()
        } catch (e: SecurityException) {
            Timber.e(e, "Security exception during discovery")
            false
        }
    }

    /**
     * Stop device discovery
     */
    fun stopDiscovery(): Boolean {
        if (!hasPermissions() || bluetoothAdapter == null) {
            return false
        }

        return try {
            if (bluetoothAdapter.isDiscovering) {
                bluetoothAdapter.cancelDiscovery()
            }
            true
        } catch (e: SecurityException) {
            Timber.e(e, "Security exception stopping discovery")
            false
        }
    }

    /**
     * Add discovered device to list
     */
    fun addDiscoveredDevice(device: BluetoothDevice) {
        val currentDevices = _discoveredDevices.value.toMutableList()
        if (!currentDevices.contains(device)) {
            currentDevices.add(device)
            _discoveredDevices.value = currentDevices
        }
    }

    /**
     * Start server mode to accept incoming connections
     */
    fun startServer(): Boolean {
        if (!hasPermissions() || bluetoothAdapter == null) {
            return false
        }

        return try {
            serverSocket = bluetoothAdapter.listenUsingRfcommWithServiceRecord(APP_NAME, APP_UUID)
            isServer = true
            
            connectionJob = scope.launch {
                acceptConnections()
            }
            
            _connectionState.value = ConnectionState.LISTENING
            true
        } catch (e: IOException) {
            Timber.e(e, "Failed to start server")
            false
        } catch (e: SecurityException) {
            Timber.e(e, "Security exception starting server")
            false
        }
    }

    /**
     * Connect to a specific device
     */
    fun connectToDevice(device: BluetoothDevice): Boolean {
        if (!hasPermissions() || bluetoothAdapter == null) {
            return false
        }

        // Cancel discovery to improve connection performance
        stopDiscovery()

        return try {
            clientSocket = device.createRfcommSocketToServiceRecord(APP_UUID)
            isServer = false
            
            connectionJob = scope.launch {
                establishConnection(device)
            }
            
            _connectionState.value = ConnectionState.CONNECTING
            true
        } catch (e: IOException) {
            Timber.e(e, "Failed to create client socket")
            false
        } catch (e: SecurityException) {
            Timber.e(e, "Security exception connecting to device")
            false
        }
    }

    /**
     * Accept incoming connections (server mode)
     */
    private suspend fun acceptConnections() {
        while (isActive && serverSocket != null) {
            try {
                val socket = withTimeout(CONNECTION_TIMEOUT) {
                    serverSocket?.accept()
                }
                
                socket?.let { connectedSocket ->
                    clientSocket = connectedSocket
                    _connectedDevice.value = connectedSocket.remoteDevice
                    _connectionState.value = ConnectionState.CONNECTED
                    
                    setupStreams(connectedSocket)
                    startMessageHandling()
                    performKeyExchange()
                }
            } catch (e: IOException) {
                Timber.e(e, "Error accepting connection")
                break
            } catch (e: TimeoutCancellationException) {
                Timber.d("Connection timeout, continuing to listen")
            }
        }
    }

    /**
     * Establish connection to device (client mode)
     */
    private suspend fun establishConnection(device: BluetoothDevice) {
        try {
            withTimeout(CONNECTION_TIMEOUT) {
                clientSocket?.connect()
            }
            
            clientSocket?.let { socket ->
                _connectedDevice.value = device
                _connectionState.value = ConnectionState.CONNECTED
                
                setupStreams(socket)
                startMessageHandling()
                performKeyExchange()
            }
        } catch (e: IOException) {
            Timber.e(e, "Failed to connect to device")
            _connectionState.value = ConnectionState.DISCONNECTED
            attemptReconnection(device)
        } catch (e: TimeoutCancellationException) {
            Timber.e("Connection timeout")
            _connectionState.value = ConnectionState.DISCONNECTED
            attemptReconnection(device)
        }
    }

    /**
     * Setup input/output streams
     */
    private fun setupStreams(socket: BluetoothSocket) {
        inputStream = socket.inputStream
        outputStream = socket.outputStream
    }

    /**
     * Start message handling coroutines
     */
    private fun startMessageHandling() {
        messageHandlingJob = scope.launch {
            launch { handleIncomingMessages() }
            launch { handleOutgoingMessages() }
        }
    }

    /**
     * Handle incoming messages
     */
    private suspend fun handleIncomingMessages() {
        val buffer = ByteArray(BUFFER_SIZE)
        
        while (isActive && inputStream != null) {
            try {
                val bytesRead = inputStream?.read(buffer) ?: -1
                if (bytesRead > 0) {
                    val messageData = buffer.copyOf(bytesRead)
                    val encryptedMessage = deserializeMessage(messageData)
                    incomingMessages.send(encryptedMessage)
                }
            } catch (e: IOException) {
                Timber.e(e, "Error reading from input stream")
                handleConnectionError()
                break
            }
        }
    }

    /**
     * Handle outgoing messages
     */
    private suspend fun handleOutgoingMessages() {
        while (isActive) {
            try {
                val message = outgoingMessages.receive()
                val messageData = serializeMessage(message)
                outputStream?.write(messageData)
                outputStream?.flush()
            } catch (e: IOException) {
                Timber.e(e, "Error writing to output stream")
                handleConnectionError()
                break
            }
        }
    }

    /**
     * Perform key exchange with connected device
     */
    private suspend fun performKeyExchange() {
        try {
            // Generate ECDH key pair
            val keyPair = cryptoManager.generateECDHKeyPair()
            
            // Send public key to peer
            val publicKeyBytes = keyPair.public.encoded
            val handshakeMessage = cryptoManager.encryptMessage(publicKeyBytes, MessageType.HANDSHAKE)
            outgoingMessages.send(handshakeMessage)
            
            // Receive peer's public key
            val peerHandshake = incomingMessages.receive()
            if (peerHandshake.messageType == MessageType.HANDSHAKE) {
                val peerPublicKeyBytes = cryptoManager.decryptMessage(peerHandshake)
                
                // Derive shared secret and session keys
                val sharedSecret = cryptoManager.deriveSharedSecret(keyPair.private, 
                    java.security.KeyFactory.getInstance("EC").generatePublic(
                        java.security.spec.X509EncodedKeySpec(peerPublicKeyBytes)
                    )
                )
                
                val salt = cryptoManager.generateSalt()
                val (aesKey, hmacKey) = cryptoManager.deriveSessionKeys(sharedSecret, salt)
                cryptoManager.setSessionKeys(aesKey, hmacKey)
                
                Timber.d("Key exchange completed successfully")
            }
        } catch (e: Exception) {
            Timber.e(e, "Key exchange failed")
            disconnect()
        }
    }

    /**
     * Send encrypted message
     */
    suspend fun sendMessage(message: ByteArray, messageType: MessageType): Boolean {
        return try {
            val encryptedMessage = cryptoManager.encryptMessage(message, messageType)
            outgoingMessages.send(encryptedMessage)
            true
        } catch (e: Exception) {
            Timber.e(e, "Failed to send message")
            false
        }
    }

    /**
     * Receive encrypted messages
     */
    fun receiveMessages(): Flow<EncryptedMessage> {
        return incomingMessages.receiveAsFlow()
    }

    /**
     * Handle connection errors
     */
    private fun handleConnectionError() {
        _connectionState.value = ConnectionState.DISCONNECTED
        
        // Attempt reconnection if we were connected
        _connectedDevice.value?.let { device ->
            scope.launch {
                attemptReconnection(device)
            }
        }
    }

    /**
     * Attempt to reconnect to device
     */
    private suspend fun attemptReconnection(device: BluetoothDevice) {
        if (reconnectAttempts < MAX_RECONNECT_ATTEMPTS) {
            reconnectAttempts++
            Timber.d("Attempting reconnection $reconnectAttempts/$MAX_RECONNECT_ATTEMPTS")
            
            delay(RECONNECT_DELAY)
            
            if (connectToDevice(device)) {
                reconnectAttempts = 0
            } else {
                attemptReconnection(device)
            }
        } else {
            Timber.w("Maximum reconnection attempts reached")
            _connectionState.value = ConnectionState.DISCONNECTED
            reconnectAttempts = 0
        }
    }

    /**
     * Disconnect from current device
     */
    fun disconnect() {
        try {
            connectionJob?.cancel()
            messageHandlingJob?.cancel()
            
            inputStream?.close()
            outputStream?.close()
            clientSocket?.close()
            serverSocket?.close()
            
            cryptoManager.clearSession()
            
            _connectionState.value = ConnectionState.DISCONNECTED
            _connectedDevice.value = null
            
            reconnectAttempts = 0
        } catch (e: IOException) {
            Timber.e(e, "Error during disconnect")
        }
    }

    /**
     * Serialize encrypted message for transmission
     */
    private fun serializeMessage(message: EncryptedMessage): ByteArray {
        // Simple serialization - in production, use more robust format
        return message.header + message.iv + message.ciphertext + message.authTag
    }

    /**
     * Deserialize received message data
     */
    private fun deserializeMessage(data: ByteArray): EncryptedMessage {
        // Simple deserialization - in production, use more robust format
        val header = data.copyOfRange(0, 16)
        val iv = data.copyOfRange(16, 28)
        val ciphertext = data.copyOfRange(28, data.size - 32)
        val authTag = data.copyOfRange(data.size - 32, data.size)
        
        return EncryptedMessage(
            version = 1,
            messageType = MessageType.TEXT_MESSAGE, // Parse from header
            sequenceNumber = 0L, // Parse from header
            iv = iv,
            ciphertext = ciphertext,
            authTag = authTag,
            header = header
        )
    }

    /**
     * Get paired devices
     */
    fun getPairedDevices(): List<BluetoothDevice> {
        return if (hasPermissions() && bluetoothAdapter != null) {
            try {
                bluetoothAdapter.bondedDevices.toList()
            } catch (e: SecurityException) {
                Timber.e(e, "Security exception getting paired devices")
                emptyList()
            }
        } else {
            emptyList()
        }
    }

    /**
     * Make device discoverable
     */
    fun makeDiscoverable(duration: Int = 300): Boolean {
        // This requires user interaction through system dialog
        return true // Implementation depends on UI layer
    }

    /**
     * Cleanup resources
     */
    fun cleanup() {
        disconnect()
        scope.cancel()
    }
}

/**
 * Connection states
 */
enum class ConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    LISTENING,
    ERROR
}

/**
 * Extension function to receive as Flow
 */
fun <T> Channel<T>.receiveAsFlow(): Flow<T> = kotlinx.coroutines.flow.flow {
    while (true) {
        emit(receive())
    }
}