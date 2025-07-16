package com.securebluetooth.chat.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class ConnectionViewModel : ViewModel() {
    private val _isConnected = MutableLiveData<Boolean>(false)
    val isConnected: LiveData<Boolean> = _isConnected

    fun setConnected(connected: Boolean) {
        _isConnected.value = connected
    }
}