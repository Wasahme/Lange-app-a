package com.bluetoothchat.encrypted.ui.main

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.bluetoothchat.encrypted.R
import com.bluetoothchat.encrypted.bluetooth.ConnectionState
import com.bluetoothchat.encrypted.databinding.ActivityMainBinding
import com.bluetoothchat.encrypted.ui.chat.ChatActivity
import com.bluetoothchat.encrypted.ui.main.adapter.DeviceListAdapter
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Main activity for device discovery and connection management
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()
    private lateinit var deviceAdapter: DeviceListAdapter

    // Permission request launcher
    private val enableBluetoothLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            initializeBluetooth()
        } else {
            showError("Bluetooth is required for this application")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        setupObservers()
        requestPermissions()
    }

    /**
     * Setup UI components
     */
    private fun setupUI() {
        // Setup toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = getString(R.string.app_name)

        // Setup device list
        deviceAdapter = DeviceListAdapter { device ->
            viewModel.connectToDevice(device)
        }
        
        binding.recyclerViewDevices.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = deviceAdapter
        }

        // Setup buttons
        binding.buttonStartDiscovery.setOnClickListener {
            viewModel.startDiscovery()
        }

        binding.buttonStopDiscovery.setOnClickListener {
            viewModel.stopDiscovery()
        }

        binding.buttonMakeDiscoverable.setOnClickListener {
            makeDeviceDiscoverable()
        }

        binding.buttonStartServer.setOnClickListener {
            viewModel.startServer()
        }

        binding.fabRefresh.setOnClickListener {
            viewModel.refreshDevices()
        }
    }

    /**
     * Setup observers for ViewModel
     */
    private fun setupObservers() {
        lifecycleScope.launch {
            // Observe discovered devices
            viewModel.discoveredDevices.collectLatest { devices ->
                deviceAdapter.updateDevices(devices)
                updateDeviceCount(devices.size)
            }
        }

        lifecycleScope.launch {
            // Observe connection state
            viewModel.connectionState.collectLatest { state ->
                updateConnectionState(state)
                
                if (state == ConnectionState.CONNECTED) {
                    // Navigate to chat activity
                    val intent = Intent(this@MainActivity, ChatActivity::class.java)
                    startActivity(intent)
                }
            }
        }

        lifecycleScope.launch {
            // Observe paired devices
            viewModel.pairedDevices.collectLatest { devices ->
                deviceAdapter.updatePairedDevices(devices)
            }
        }

        lifecycleScope.launch {
            // Observe discovery state
            viewModel.isDiscovering.collectLatest { isDiscovering ->
                updateDiscoveryState(isDiscovering)
            }
        }

        lifecycleScope.launch {
            // Observe error messages
            viewModel.errorMessage.collectLatest { message ->
                if (message.isNotEmpty()) {
                    showError(message)
                }
            }
        }
    }

    /**
     * Request necessary permissions
     */
    private fun requestPermissions() {
        val permissions = mutableListOf<String>().apply {
            // Bluetooth permissions
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                add(Manifest.permission.BLUETOOTH_CONNECT)
                add(Manifest.permission.BLUETOOTH_SCAN)
                add(Manifest.permission.BLUETOOTH_ADVERTISE)
            } else {
                add(Manifest.permission.BLUETOOTH)
                add(Manifest.permission.BLUETOOTH_ADMIN)
            }
            
            // Location permissions (required for device discovery)
            add(Manifest.permission.ACCESS_COARSE_LOCATION)
            add(Manifest.permission.ACCESS_FINE_LOCATION)
            
            // Audio permissions (for voice calls)
            add(Manifest.permission.RECORD_AUDIO)
            add(Manifest.permission.MODIFY_AUDIO_SETTINGS)
        }

        Dexter.withContext(this)
            .withPermissions(permissions)
            .withListener(object : MultiplePermissionsListener {
                override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                    report?.let {
                        if (it.areAllPermissionsGranted()) {
                            checkBluetoothEnabled()
                        } else {
                            showError("Permissions are required for the application to work")
                            finish()
                        }
                    }
                }

                override fun onPermissionRationaleShouldBeShown(
                    permissions: MutableList<PermissionRequest>?,
                    token: PermissionToken?
                ) {
                    token?.continuePermissionRequest()
                }
            })
            .check()
    }

    /**
     * Check if Bluetooth is enabled
     */
    private fun checkBluetoothEnabled() {
        if (!viewModel.isBluetoothEnabled()) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            enableBluetoothLauncher.launch(enableBtIntent)
        } else {
            initializeBluetooth()
        }
    }

    /**
     * Initialize Bluetooth functionality
     */
    private fun initializeBluetooth() {
        if (viewModel.initializeBluetooth()) {
            Timber.d("Bluetooth initialized successfully")
            viewModel.loadPairedDevices()
        } else {
            showError("Failed to initialize Bluetooth")
        }
    }

    /**
     * Make device discoverable
     */
    private fun makeDeviceDiscoverable() {
        if (ActivityCompat.checkSelfPermission(
                this,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    Manifest.permission.BLUETOOTH_ADVERTISE
                } else {
                    Manifest.permission.BLUETOOTH_ADMIN
                }
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            showError("Bluetooth permissions not granted")
            return
        }

        val discoverableIntent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE).apply {
            putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300)
        }
        startActivity(discoverableIntent)
    }

    /**
     * Update device count display
     */
    private fun updateDeviceCount(count: Int) {
        binding.textViewDeviceCount.text = getString(R.string.devices_found, count)
    }

    /**
     * Update connection state display
     */
    private fun updateConnectionState(state: ConnectionState) {
        val statusText = when (state) {
            ConnectionState.DISCONNECTED -> getString(R.string.status_disconnected)
            ConnectionState.CONNECTING -> getString(R.string.status_connecting)
            ConnectionState.CONNECTED -> getString(R.string.status_connected)
            ConnectionState.LISTENING -> getString(R.string.status_listening)
            ConnectionState.ERROR -> getString(R.string.status_error)
        }
        
        binding.textViewConnectionStatus.text = statusText
        
        // Update UI based on connection state
        binding.buttonStartServer.isEnabled = state == ConnectionState.DISCONNECTED
        binding.buttonStartDiscovery.isEnabled = state == ConnectionState.DISCONNECTED || state == ConnectionState.LISTENING
    }

    /**
     * Update discovery state display
     */
    private fun updateDiscoveryState(isDiscovering: Boolean) {
        binding.buttonStartDiscovery.isEnabled = !isDiscovering
        binding.buttonStopDiscovery.isEnabled = isDiscovering
        binding.progressBarDiscovery.visibility = if (isDiscovering) {
            android.view.View.VISIBLE
        } else {
            android.view.View.GONE
        }
    }

    /**
     * Show error message
     */
    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        Timber.e("Error: $message")
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.cleanup()
    }
}