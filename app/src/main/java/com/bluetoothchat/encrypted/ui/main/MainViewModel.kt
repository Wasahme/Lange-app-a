package com.bluetoothchat.encrypted.ui.main

import android.bluetooth.BluetoothDevice
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bluetoothchat.encrypted.bluetooth.BluetoothManager
import com.bluetoothchat.encrypted.bluetooth.ConnectionState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val bluetoothManager: BluetoothManager
) : ViewModel() {

    // Bluetooth state
    val connectionState: StateFlow<ConnectionState> = bluetoothManager.connectionState
    val discoveredDevices: StateFlow<List<BluetoothDevice>> = bluetoothManager.discoveredDevices
    val connectedDevice: StateFlow<BluetoothDevice?> = bluetoothManager.connectedDevice

    // UI state
    private val _pairedDevices = MutableStateFlow<List<BluetoothDevice>>(emptyList())
    val pairedDevices: StateFlow<List<BluetoothDevice>> = _pairedDevices.asStateFlow()

    private val _isDiscovering = MutableStateFlow(false)
    val isDiscovering: StateFlow<Boolean> = _isDiscovering.asStateFlow()

    private val _errorMessage = MutableStateFlow("")
    val errorMessage: StateFlow<String> = _errorMessage.asStateFlow()

    private val _isBluetoothEnabled = MutableStateFlow(false)
    val isBluetoothEnabled: StateFlow<Boolean> = _isBluetoothEnabled.asStateFlow()

    init {
        checkBluetoothStatus()
    }

    /**
     * Check Bluetooth availability and permissions
     */
    private fun checkBluetoothStatus() {
        _isBluetoothEnabled.value = bluetoothManager.isBluetoothAvailable()
        if (!bluetoothManager.hasPermissions()) {
            _errorMessage.value = "Bluetooth permissions required"
        }
    }

    /**
     * Initialize Bluetooth functionality
     */
    fun initializeBluetooth(): Boolean {
        return try {
            val initialized = bluetoothManager.initialize()
            _isBluetoothEnabled.value = initialized
            if (initialized) {
                loadPairedDevices()
            }
            initialized
        } catch (e: Exception) {
            Timber.e(e, "Failed to initialize Bluetooth")
            _errorMessage.value = "Failed to initialize Bluetooth: ${e.message}"
            false
        }
    }

    /**
     * Check if Bluetooth is enabled
     */
    fun isBluetoothEnabled(): Boolean {
        return bluetoothManager.isBluetoothAvailable()
    }

    /**
     * Load paired devices
     */
    fun loadPairedDevices() {
        viewModelScope.launch {
            try {
                val devices = bluetoothManager.getPairedDevices()
                _pairedDevices.value = devices
                Timber.d("Loaded ${devices.size} paired devices")
            } catch (e: Exception) {
                Timber.e(e, "Failed to load paired devices")
                _errorMessage.value = "Failed to load paired devices"
            }
        }
    }

    /**
     * Start device discovery
     */
    fun startDiscovery() {
        viewModelScope.launch {
            try {
                if (bluetoothManager.startDiscovery()) {
                    _isDiscovering.value = true
                    Timber.d("Device discovery started")
                } else {
                    _errorMessage.value = "Failed to start device discovery"
                }
            } catch (e: Exception) {
                Timber.e(e, "Error starting discovery")
                _errorMessage.value = "Error starting discovery: ${e.message}"
            }
        }
    }

    /**
     * Stop device discovery
     */
    fun stopDiscovery() {
        viewModelScope.launch {
            try {
                if (bluetoothManager.stopDiscovery()) {
                    _isDiscovering.value = false
                    Timber.d("Device discovery stopped")
                } else {
                    _errorMessage.value = "Failed to stop device discovery"
                }
            } catch (e: Exception) {
                Timber.e(e, "Error stopping discovery")
                _errorMessage.value = "Error stopping discovery: ${e.message}"
            }
        }
    }

    /**
     * Start server mode
     */
    fun startServer() {
        viewModelScope.launch {
            try {
                if (bluetoothManager.startServer()) {
                    Timber.d("Server mode started")
                } else {
                    _errorMessage.value = "Failed to start server mode"
                }
            } catch (e: Exception) {
                Timber.e(e, "Error starting server")
                _errorMessage.value = "Error starting server: ${e.message}"
            }
        }
    }

    /**
     * Connect to a specific device
     */
    fun connectToDevice(device: BluetoothDevice) {
        viewModelScope.launch {
            try {
                if (bluetoothManager.connectToDevice(device)) {
                    Timber.d("Connecting to device: ${device.name}")
                } else {
                    _errorMessage.value = "Failed to connect to device"
                }
            } catch (e: Exception) {
                Timber.e(e, "Error connecting to device")
                _errorMessage.value = "Error connecting to device: ${e.message}"
            }
        }
    }

    /**
     * Disconnect from current device
     */
    fun disconnect() {
        viewModelScope.launch {
            try {
                bluetoothManager.disconnect()
                Timber.d("Disconnected from device")
            } catch (e: Exception) {
                Timber.e(e, "Error disconnecting")
                _errorMessage.value = "Error disconnecting: ${e.message}"
            }
        }
    }

    /**
     * Refresh device lists
     */
    fun refreshDevices() {
        loadPairedDevices()
        if (_isDiscovering.value) {
            stopDiscovery()
        }
        startDiscovery()
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
        bluetoothManager.cleanup()
    }

    override fun onCleared() {
        super.onCleared()
        cleanup()
    }
}