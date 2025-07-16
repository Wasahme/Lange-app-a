package com.bluetoothchat.encrypted.ui.chat.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bluetoothchat.encrypted.R
import com.bluetoothchat.encrypted.data.model.Message
import com.bluetoothchat.encrypted.data.model.MessageStatus
import com.bluetoothchat.encrypted.data.model.MessageType
import com.bluetoothchat.encrypted.data.model.getDisplayText
import com.bluetoothchat.encrypted.databinding.ItemMessageReceivedBinding
import com.bluetoothchat.encrypted.databinding.ItemMessageSentBinding
import com.bluetoothchat.encrypted.databinding.ItemMessageSystemBinding

class MessageAdapter : ListAdapter<Message, RecyclerView.ViewHolder>(MessageDiffCallback()) {

    companion object {
        private const val VIEW_TYPE_SENT = 1
        private const val VIEW_TYPE_RECEIVED = 2
        private const val VIEW_TYPE_SYSTEM = 3
    }

    override fun getItemViewType(position: Int): Int {
        val message = getItem(position)
        return when {
            message.messageType == MessageType.SYSTEM -> VIEW_TYPE_SYSTEM
            message.isFromMe -> VIEW_TYPE_SENT
            else -> VIEW_TYPE_RECEIVED
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        
        return when (viewType) {
            VIEW_TYPE_SENT -> {
                val binding = ItemMessageSentBinding.inflate(inflater, parent, false)
                SentMessageViewHolder(binding)
            }
            VIEW_TYPE_RECEIVED -> {
                val binding = ItemMessageReceivedBinding.inflate(inflater, parent, false)
                ReceivedMessageViewHolder(binding)
            }
            VIEW_TYPE_SYSTEM -> {
                val binding = ItemMessageSystemBinding.inflate(inflater, parent, false)
                SystemMessageViewHolder(binding)
            }
            else -> throw IllegalArgumentException("Unknown view type: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = getItem(position)
        
        when (holder) {
            is SentMessageViewHolder -> holder.bind(message)
            is ReceivedMessageViewHolder -> holder.bind(message)
            is SystemMessageViewHolder -> holder.bind(message)
        }
    }

    class SentMessageViewHolder(
        private val binding: ItemMessageSentBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(message: Message) {
            binding.textViewMessage.text = message.text
            binding.textViewTime.text = message.getFormattedTime()
            binding.textViewStatus.text = message.status.getDisplayText()
            
            // Show encryption indicator
            if (message.isEncrypted) {
                binding.iconEncryption.visibility = android.view.View.VISIBLE
                binding.iconEncryption.setImageResource(R.drawable.ic_lock)
            } else {
                binding.iconEncryption.visibility = android.view.View.GONE
            }
            
            // Update status color
            val statusColor = when (message.status) {
                MessageStatus.SENDING -> R.color.design_default_color_on_surface_variant
                MessageStatus.SENT -> R.color.design_default_color_primary
                MessageStatus.DELIVERED -> R.color.design_default_color_primary
                MessageStatus.READ -> R.color.design_default_color_primary
                MessageStatus.FAILED -> R.color.design_default_color_error
                MessageStatus.RECEIVED -> R.color.design_default_color_primary
            }
            
            binding.textViewStatus.setTextColor(
                binding.root.context.getColor(statusColor)
            )
            
            // Show/hide status indicator
            binding.imageViewStatus.visibility = if (message.status == MessageStatus.FAILED) {
                binding.imageViewStatus.setImageResource(R.drawable.ic_error)
                android.view.View.VISIBLE
            } else {
                android.view.View.GONE
            }
        }
    }

    class ReceivedMessageViewHolder(
        private val binding: ItemMessageReceivedBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(message: Message) {
            binding.textViewMessage.text = message.text
            binding.textViewTime.text = message.getFormattedTime()
            
            // Show encryption indicator
            if (message.isEncrypted) {
                binding.iconEncryption.visibility = android.view.View.VISIBLE
                binding.iconEncryption.setImageResource(R.drawable.ic_lock)
            } else {
                binding.iconEncryption.visibility = android.view.View.GONE
            }
            
            // Show recent message indicator
            if (message.isRecent()) {
                binding.viewNewMessageIndicator.visibility = android.view.View.VISIBLE
            } else {
                binding.viewNewMessageIndicator.visibility = android.view.View.GONE
            }
        }
    }

    class SystemMessageViewHolder(
        private val binding: ItemMessageSystemBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(message: Message) {
            binding.textViewSystemMessage.text = message.text
            binding.textViewTime.text = message.getFormattedTime()
            
            // Set appropriate icon based on message content
            val iconResource = when {
                message.text.contains("Connected") -> R.drawable.ic_bluetooth_connected
                message.text.contains("Disconnected") -> R.drawable.ic_bluetooth_disabled
                message.text.contains("encryption") -> R.drawable.ic_lock
                message.text.contains("Error") -> R.drawable.ic_error
                else -> R.drawable.ic_info
            }
            
            binding.iconSystem.setImageResource(iconResource)
        }
    }
}

class MessageDiffCallback : DiffUtil.ItemCallback<Message>() {
    override fun areItemsTheSame(oldItem: Message, newItem: Message): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Message, newItem: Message): Boolean {
        return oldItem == newItem
    }
}