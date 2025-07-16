package com.securebluetooth.chat.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class ChatViewModel : ViewModel() {
    private val _messages = MutableLiveData<List<String>>()
    val messages: LiveData<List<String>> = _messages

    fun sendMessage(msg: String) {
        val current = _messages.value?.toMutableList() ?: mutableListOf()
        current.add(msg)
        _messages.value = current
    }
}