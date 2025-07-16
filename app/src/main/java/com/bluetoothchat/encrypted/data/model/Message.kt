package com.bluetoothchat.encrypted.data.model

import java.text.SimpleDateFormat
import java.util.*

/**
 * Data class representing a chat message
 */
data class Message(
    val id: String,
    val text: String,
    val timestamp: Long,
    val isFromMe: Boolean,
    val status: MessageStatus,
    val isEncrypted: Boolean = false,
    val messageType: MessageType = MessageType.TEXT
) {
    /**
     * Get formatted timestamp
     */
    fun getFormattedTime(): String {
        val date = Date(timestamp)
        val format = SimpleDateFormat("HH:mm", Locale.getDefault())
        return format.format(date)
    }

    /**
     * Get formatted date
     */
    fun getFormattedDate(): String {
        val date = Date(timestamp)
        val format = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        return format.format(date)
    }

    /**
     * Check if message is from today
     */
    fun isFromToday(): Boolean {
        val today = Calendar.getInstance()
        val messageDate = Calendar.getInstance().apply {
            timeInMillis = timestamp
        }
        
        return today.get(Calendar.YEAR) == messageDate.get(Calendar.YEAR) &&
               today.get(Calendar.DAY_OF_YEAR) == messageDate.get(Calendar.DAY_OF_YEAR)
    }

    /**
     * Check if message is recent (within last 5 minutes)
     */
    fun isRecent(): Boolean {
        val currentTime = System.currentTimeMillis()
        val fiveMinutesAgo = currentTime - (5 * 60 * 1000)
        return timestamp >= fiveMinutesAgo
    }
}

/**
 * Enum representing message status
 */
enum class MessageStatus {
    SENDING,
    SENT,
    DELIVERED,
    READ,
    FAILED,
    RECEIVED
}

/**
 * Enum representing message type
 */
enum class MessageType {
    TEXT,
    VOICE,
    IMAGE,
    FILE,
    SYSTEM
}

/**
 * Extension functions for MessageStatus
 */
fun MessageStatus.getDisplayText(): String {
    return when (this) {
        MessageStatus.SENDING -> "Sending..."
        MessageStatus.SENT -> "Sent"
        MessageStatus.DELIVERED -> "Delivered"
        MessageStatus.READ -> "Read"
        MessageStatus.FAILED -> "Failed"
        MessageStatus.RECEIVED -> "Received"
    }
}

fun MessageStatus.getIconResource(): Int {
    return when (this) {
        MessageStatus.SENDING -> android.R.drawable.ic_menu_upload
        MessageStatus.SENT -> android.R.drawable.ic_menu_send
        MessageStatus.DELIVERED -> android.R.drawable.ic_menu_info_details
        MessageStatus.READ -> android.R.drawable.ic_menu_view
        MessageStatus.FAILED -> android.R.drawable.ic_menu_close_clear_cancel
        MessageStatus.RECEIVED -> android.R.drawable.ic_menu_info_details
    }
}

/**
 * Helper class for creating system messages
 */
object SystemMessage {
    fun createConnectionMessage(deviceName: String, connected: Boolean): Message {
        val text = if (connected) {
            "Connected to $deviceName"
        } else {
            "Disconnected from $deviceName"
        }
        
        return Message(
            id = UUID.randomUUID().toString(),
            text = text,
            timestamp = System.currentTimeMillis(),
            isFromMe = false,
            status = MessageStatus.RECEIVED,
            isEncrypted = false,
            messageType = MessageType.SYSTEM
        )
    }

    fun createEncryptionMessage(enabled: Boolean): Message {
        val text = if (enabled) {
            "End-to-end encryption enabled"
        } else {
            "Encryption disabled"
        }
        
        return Message(
            id = UUID.randomUUID().toString(),
            text = text,
            timestamp = System.currentTimeMillis(),
            isFromMe = false,
            status = MessageStatus.RECEIVED,
            isEncrypted = false,
            messageType = MessageType.SYSTEM
        )
    }

    fun createErrorMessage(error: String): Message {
        return Message(
            id = UUID.randomUUID().toString(),
            text = "Error: $error",
            timestamp = System.currentTimeMillis(),
            isFromMe = false,
            status = MessageStatus.RECEIVED,
            isEncrypted = false,
            messageType = MessageType.SYSTEM
        )
    }
}