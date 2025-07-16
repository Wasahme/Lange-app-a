package com.securebluetooth.chat.repository

class MessageRepository {
    private val messages = mutableListOf<String>()

    fun addMessage(msg: String) {
        messages.add(msg)
    }

    fun getMessages(): List<String> = messages
}