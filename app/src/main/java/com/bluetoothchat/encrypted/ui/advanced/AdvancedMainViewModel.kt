package com.bluetoothchat.encrypted.ui.advanced

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bluetoothchat.encrypted.ai.AIManager
import com.bluetoothchat.encrypted.audio.AdvancedAudioManager
import com.bluetoothchat.encrypted.bluetooth.AdvancedBluetoothManager
import com.bluetoothchat.encrypted.bluetooth.DeviceDiscoveryResult
import com.bluetoothchat.encrypted.bluetooth.MeshTopology
import com.bluetoothchat.encrypted.bluetooth.NetworkMetrics
import com.bluetoothchat.encrypted.crypto.AdvancedCryptoManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class AdvancedMainViewModel @Inject constructor(
    private val bluetoothManager: AdvancedBluetoothManager,
    private val cryptoManager: AdvancedCryptoManager,
    private val audioManager: AdvancedAudioManager,
    private val aiManager: AIManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdvancedMainUiState())
    val uiState: StateFlow<AdvancedMainUiState> = _uiState.asStateFlow()

    private val _discoveredDevices = MutableStateFlow<List<DeviceDiscoveryResult.DeviceFound>>(emptyList())
    val discoveredDevices: StateFlow<List<DeviceDiscoveryResult.DeviceFound>> = _discoveredDevices.asStateFlow()

    val networkMetrics: StateFlow<NetworkMetrics> = bluetoothManager.networkMetrics
    val meshTopology: StateFlow<MeshTopology> = bluetoothManager.meshTopology

    init {
        observeBluetoothState()
        observeNetworkMetrics()
        initializeSettings()
    }

    private fun observeBluetoothState() {
        viewModelScope.launch {
            bluetoothManager.connectionState.collect { state ->
                _uiState.value = _uiState.value.copy(
                    connectionState = state
                )
            }
        }
    }

    private fun observeNetworkMetrics() {
        viewModelScope.launch {
            bluetoothManager.networkMetrics.collect { metrics ->
                _uiState.value = _uiState.value.copy(
                    networkMetrics = metrics
                )
            }
        }

        viewModelScope.launch {
            bluetoothManager.meshTopology.collect { topology ->
                _uiState.value = _uiState.value.copy(
                    meshTopology = topology
                )
            }
        }
    }

    private fun initializeSettings() {
        _uiState.value = _uiState.value.copy(
            settings = AdvancedSettings(
                encryptionAlgorithm = cryptoManager.getCurrentAlgorithm(),
                audioCodec = "OPUS",
                noiseReduction = true,
                meshNetworking = true,
                voiceBiometrics = false,
                adaptiveQuality = true
            )
        )
    }

    fun toggleScanning() {
        viewModelScope.launch {
            if (_uiState.value.isScanning) {
                stopScanning()
            } else {
                startScanning()
            }
        }
    }

    private suspend fun startScanning() {
        _uiState.value = _uiState.value.copy(isScanning = true)
        _discoveredDevices.value = emptyList()

        try {
            bluetoothManager.startAdvancedDiscovery().collect { result ->
                when (result) {
                    is DeviceDiscoveryResult.Started -> {
                        Timber.d("Device discovery started")
                    }
                    is DeviceDiscoveryResult.DeviceFound -> {
                        val currentDevices = _discoveredDevices.value.toMutableList()
                        val existingIndex = currentDevices.indexOfFirst { 
                            it.device.address == result.device.address 
                        }
                        
                        if (existingIndex >= 0) {
                            currentDevices[existingIndex] = result
                        } else {
                            currentDevices.add(result)
                        }
                        
                        _discoveredDevices.value = currentDevices
                    }
                    is DeviceDiscoveryResult.Error -> {
                        Timber.e("Discovery error: ${result.message}")
                        _uiState.value = _uiState.value.copy(
                            isScanning = false,
                            error = result.message
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error during device discovery")
            _uiState.value = _uiState.value.copy(
                isScanning = false,
                error = e.message
            )
        }
    }

    private fun stopScanning() {
        _uiState.value = _uiState.value.copy(isScanning = false)
    }

    fun connectToDevice(device: android.bluetooth.BluetoothDevice) {
        viewModelScope.launch {
            try {
                bluetoothManager.connectToDevice(device).collect { result ->
                    when (result) {
                        is com.bluetoothchat.encrypted.bluetooth.ConnectionResult.Connecting -> {
                            Timber.d("Connecting to device: ${device.address}")
                        }
                        is com.bluetoothchat.encrypted.bluetooth.ConnectionResult.Connected -> {
                            Timber.d("Connected to device: ${device.address}")
                            _uiState.value = _uiState.value.copy(
                                connectedDevice = device
                            )
                        }
                        is com.bluetoothchat.encrypted.bluetooth.ConnectionResult.Failed -> {
                            Timber.e("Connection failed: ${result.error}")
                            _uiState.value = _uiState.value.copy(
                                error = result.error
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error connecting to device")
                _uiState.value = _uiState.value.copy(
                    error = e.message
                )
            }
        }
    }

    fun updateSetting(setting: String, value: Any) {
        val currentSettings = _uiState.value.settings
        val newSettings = when (setting) {
            "encryptionAlgorithm" -> {
                cryptoManager.setEncryptionAlgorithm(value as String)
                currentSettings.copy(encryptionAlgorithm = value)
            }
            "audioCodec" -> {
                audioManager.setAudioCodec(value as String)
                currentSettings.copy(audioCodec = value)
            }
            "noiseReduction" -> {
                val level = if (value as Boolean) 
                    com.bluetoothchat.encrypted.audio.NoiseReductionLevel.MEDIUM 
                else 
                    com.bluetoothchat.encrypted.audio.NoiseReductionLevel.OFF
                audioManager.setNoiseReductionLevel(level)
                currentSettings.copy(noiseReduction = value)
            }
            "meshNetworking" -> currentSettings.copy(meshNetworking = value as Boolean)
            "voiceBiometrics" -> {
                audioManager.setVoiceBiometricsEnabled(value as Boolean)
                currentSettings.copy(voiceBiometrics = value)
            }
            "adaptiveQuality" -> currentSettings.copy(adaptiveQuality = value as Boolean)
            else -> currentSettings
        }

        _uiState.value = _uiState.value.copy(settings = newSettings)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    override fun onCleared() {
        super.onCleared()
        bluetoothManager.cleanup()
        audioManager.cleanup()
        aiManager.cleanup()
    }
}

data class AdvancedMainUiState(
    val isScanning: Boolean = false,
    val connectionState: com.bluetoothchat.encrypted.bluetooth.BluetoothState = com.bluetoothchat.encrypted.bluetooth.BluetoothState.DISCONNECTED,
    val connectedDevice: android.bluetooth.BluetoothDevice? = null,
    val networkMetrics: NetworkMetrics = NetworkMetrics(),
    val meshTopology: MeshTopology = MeshTopology(),
    val settings: AdvancedSettings = AdvancedSettings(),
    val error: String? = null
)