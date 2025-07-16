package com.securebluetooth.chat

import android.bluetooth.*
import android.content.Context
import java.io.IOException
import java.util.UUID

class BluetoothManager(private val context: Context) {
    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private var serverSocket: BluetoothServerSocket? = null
    private var clientSocket: BluetoothSocket? = null
    private val appUuid: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    fun startDiscovery() {
        bluetoothAdapter?.startDiscovery()
    }

    fun connectToDevice(device: BluetoothDevice) {
        clientSocket = device.createRfcommSocketToServiceRecord(appUuid)
        bluetoothAdapter?.cancelDiscovery()
        clientSocket?.connect()
    }

    fun startServer() {
        serverSocket = bluetoothAdapter?.listenUsingRfcommWithServiceRecord("SecureBluetoothChat", appUuid)
    }

    fun sendData(data: ByteArray) {
        clientSocket?.outputStream?.write(data)
    }

    fun receiveData(): ByteArray? {
        val buffer = ByteArray(1024)
        val bytes = clientSocket?.inputStream?.read(buffer) ?: -1
        return if (bytes > 0) buffer.copyOf(bytes) else null
    }
}