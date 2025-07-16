package com.securebluetooth.chat.repository

class CallRepository {
    private var isInCall: Boolean = false

    fun startCall() {
        isInCall = true
    }

    fun endCall() {
        isInCall = false
    }

    fun isCallActive(): Boolean = isInCall
}