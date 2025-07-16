package com.bluetoothchat.encrypted.ui.main.adapter

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bluetoothchat.encrypted.R
import com.bluetoothchat.encrypted.databinding.ItemDeviceBinding

class DeviceListAdapter(
    private val onDeviceClick: (BluetoothDevice) -> Unit
) : ListAdapter<DeviceItem, DeviceListAdapter.DeviceViewHolder>(DeviceDiffCallback()) {

    private var pairedDevices: List<BluetoothDevice> = emptyList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
        val binding = ItemDeviceBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return DeviceViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
        holder.bind(getItem(position), onDeviceClick)
    }

    fun updateDevices(discoveredDevices: List<BluetoothDevice>) {
        val deviceItems = mutableListOf<DeviceItem>()
        
        // Add paired devices first
        pairedDevices.forEach { device ->
            deviceItems.add(DeviceItem(device, DeviceType.PAIRED))
        }
        
        // Add discovered devices that are not already paired
        discoveredDevices.forEach { device ->
            if (!pairedDevices.contains(device)) {
                deviceItems.add(DeviceItem(device, DeviceType.DISCOVERED))
            }
        }
        
        submitList(deviceItems)
    }

    fun updatePairedDevices(devices: List<BluetoothDevice>) {
        pairedDevices = devices
        // Refresh the list with current discovered devices
        val currentDiscovered = currentList.filter { it.type == DeviceType.DISCOVERED }
            .map { it.device }
        updateDevices(currentDiscovered)
    }

    class DeviceViewHolder(
        private val binding: ItemDeviceBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        @SuppressLint("MissingPermission")
        fun bind(deviceItem: DeviceItem, onDeviceClick: (BluetoothDevice) -> Unit) {
            val device = deviceItem.device
            val context = binding.root.context
            
            // Device name
            binding.textViewDeviceName.text = device.name ?: "Unknown Device"
            
            // Device address
            binding.textViewDeviceAddress.text = device.address
            
            // Device type and pairing status
            when (deviceItem.type) {
                DeviceType.PAIRED -> {
                    binding.textViewDeviceStatus.text = context.getString(R.string.device_paired)
                    binding.textViewDeviceStatus.setTextColor(
                        context.getColor(R.color.design_default_color_primary)
                    )
                    binding.iconDeviceType.setImageResource(R.drawable.ic_bluetooth_connected)
                }
                DeviceType.DISCOVERED -> {
                    binding.textViewDeviceStatus.text = context.getString(R.string.device_not_paired)
                    binding.textViewDeviceStatus.setTextColor(
                        context.getColor(R.color.design_default_color_on_surface)
                    )
                    binding.iconDeviceType.setImageResource(R.drawable.ic_bluetooth)
                }
            }
            
            // Signal strength (simulated)
            val signalStrength = getSignalStrength(device)
            binding.textViewSignalStrength.text = context.getString(
                R.string.signal_strength, signalStrength
            )
            
            // Device class icon
            binding.iconDeviceClass.setImageResource(getDeviceClassIcon(device))
            
            // Connect button
            binding.buttonConnect.setOnClickListener {
                onDeviceClick(device)
            }
            
            // Set click listener for entire item
            binding.root.setOnClickListener {
                onDeviceClick(device)
            }
        }

        private fun getSignalStrength(device: BluetoothDevice): String {
            // Simulate signal strength based on device type
            return when ((0..3).random()) {
                0 -> binding.root.context.getString(R.string.signal_weak)
                1 -> binding.root.context.getString(R.string.signal_medium)
                2 -> binding.root.context.getString(R.string.signal_strong)
                else -> binding.root.context.getString(R.string.signal_unknown)
            }
        }

        private fun getDeviceClassIcon(device: BluetoothDevice): Int {
            return when (device.bluetoothClass?.majorDeviceClass) {
                android.bluetooth.BluetoothClass.Device.Major.PHONE -> R.drawable.ic_phone
                android.bluetooth.BluetoothClass.Device.Major.COMPUTER -> R.drawable.ic_computer
                android.bluetooth.BluetoothClass.Device.Major.AUDIO_VIDEO -> R.drawable.ic_headphones
                else -> R.drawable.ic_bluetooth
            }
        }
    }
}

data class DeviceItem(
    val device: BluetoothDevice,
    val type: DeviceType
)

enum class DeviceType {
    PAIRED,
    DISCOVERED
}

class DeviceDiffCallback : DiffUtil.ItemCallback<DeviceItem>() {
    override fun areItemsTheSame(oldItem: DeviceItem, newItem: DeviceItem): Boolean {
        return oldItem.device.address == newItem.device.address
    }

    override fun areContentsTheSame(oldItem: DeviceItem, newItem: DeviceItem): Boolean {
        return oldItem == newItem
    }
}