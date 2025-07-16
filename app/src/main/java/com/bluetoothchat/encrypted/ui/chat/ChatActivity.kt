package com.bluetoothchat.encrypted.ui.chat

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.bluetoothchat.encrypted.R
import com.bluetoothchat.encrypted.databinding.ActivityChatBinding
import com.bluetoothchat.encrypted.ui.call.CallActivity
import com.bluetoothchat.encrypted.ui.chat.adapter.MessageAdapter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber

@AndroidEntryPoint
class ChatActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChatBinding
    private val viewModel: ChatViewModel by viewModels()
    private lateinit var messageAdapter: MessageAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        setupObservers()
        viewModel.initializeChat()
    }

    private fun setupUI() {
        // Setup toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.chat_title)

        // Setup message list
        messageAdapter = MessageAdapter()
        binding.recyclerViewMessages.apply {
            layoutManager = LinearLayoutManager(this@ChatActivity).apply {
                stackFromEnd = true
            }
            adapter = messageAdapter
        }

        // Setup send button
        binding.buttonSend.setOnClickListener {
            val message = binding.editTextMessage.text.toString().trim()
            if (message.isNotEmpty()) {
                viewModel.sendMessage(message)
                binding.editTextMessage.text.clear()
            }
        }

        // Setup voice call button
        binding.buttonVoiceCall.setOnClickListener {
            startVoiceCall()
        }

        // Setup message input
        binding.editTextMessage.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEND) {
                binding.buttonSend.performClick()
                true
            } else {
                false
            }
        }
    }

    private fun setupObservers() {
        lifecycleScope.launch {
            // Observe messages
            viewModel.messages.collectLatest { messages ->
                messageAdapter.submitList(messages)
                if (messages.isNotEmpty()) {
                    binding.recyclerViewMessages.smoothScrollToPosition(messages.size - 1)
                }
            }
        }

        lifecycleScope.launch {
            // Observe connection status
            viewModel.connectionStatus.collectLatest { status ->
                updateConnectionStatus(status)
            }
        }

        lifecycleScope.launch {
            // Observe encryption status
            viewModel.encryptionStatus.collectLatest { isEncrypted ->
                updateEncryptionStatus(isEncrypted)
            }
        }

        lifecycleScope.launch {
            // Observe connected device
            viewModel.connectedDeviceName.collectLatest { deviceName ->
                supportActionBar?.subtitle = deviceName
            }
        }

        lifecycleScope.launch {
            // Observe typing indicator
            viewModel.isTyping.collectLatest { isTyping ->
                updateTypingIndicator(isTyping)
            }
        }

        lifecycleScope.launch {
            // Observe errors
            viewModel.errorMessage.collectLatest { error ->
                if (error.isNotEmpty()) {
                    showError(error)
                }
            }
        }

        lifecycleScope.launch {
            // Observe message sending state
            viewModel.isSendingMessage.collectLatest { isSending ->
                binding.buttonSend.isEnabled = !isSending
                binding.progressBarSending.visibility = if (isSending) {
                    android.view.View.VISIBLE
                } else {
                    android.view.View.GONE
                }
            }
        }
    }

    private fun updateConnectionStatus(status: String) {
        binding.textViewConnectionStatus.text = status
        
        // Update UI based on connection status
        val isConnected = status == getString(R.string.status_connected)
        binding.editTextMessage.isEnabled = isConnected
        binding.buttonSend.isEnabled = isConnected
        binding.buttonVoiceCall.isEnabled = isConnected
        
        if (!isConnected) {
            binding.textViewConnectionStatus.setTextColor(getColor(R.color.design_default_color_error))
        } else {
            binding.textViewConnectionStatus.setTextColor(getColor(R.color.design_default_color_primary))
        }
    }

    private fun updateEncryptionStatus(isEncrypted: Boolean) {
        if (isEncrypted) {
            binding.textViewEncryptionStatus.text = getString(R.string.encryption_enabled)
            binding.textViewEncryptionStatus.setTextColor(getColor(R.color.design_default_color_primary))
            binding.iconEncryption.setImageResource(R.drawable.ic_lock)
        } else {
            binding.textViewEncryptionStatus.text = getString(R.string.encryption_disabled)
            binding.textViewEncryptionStatus.setTextColor(getColor(R.color.design_default_color_error))
            binding.iconEncryption.setImageResource(R.drawable.ic_lock_open)
        }
    }

    private fun updateTypingIndicator(isTyping: Boolean) {
        if (isTyping) {
            binding.textViewTypingIndicator.visibility = android.view.View.VISIBLE
            binding.textViewTypingIndicator.text = getString(R.string.typing_indicator, "Other user")
        } else {
            binding.textViewTypingIndicator.visibility = android.view.View.GONE
        }
    }

    private fun startVoiceCall() {
        val intent = Intent(this, CallActivity::class.java)
        startActivity(intent)
    }

    private fun showError(error: String) {
        Toast.makeText(this, error, Toast.LENGTH_LONG).show()
        Timber.e("Chat error: $error")
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.cleanup()
    }
}