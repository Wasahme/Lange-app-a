package com.securebluetooth.chat.repository

import android.bluetooth.BluetoothDevice

class DeviceRepository {
    private val devices = mutableListOf<BluetoothDevice>()

    fun addDevice(device: BluetoothDevice) {
        if (!devices.contains(device)) devices.add(device)
    }

    fun getDevices(): List<BluetoothDevice> = devices
}