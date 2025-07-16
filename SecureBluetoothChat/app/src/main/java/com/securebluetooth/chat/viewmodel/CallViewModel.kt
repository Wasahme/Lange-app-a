package com.securebluetooth.chat.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class CallViewModel : ViewModel() {
    private val _callStatus = MutableLiveData<String>("جاري الاتصال...")
    val callStatus: LiveData<String> = _callStatus

    fun updateStatus(status: String) {
        _callStatus.value = status
    }
}