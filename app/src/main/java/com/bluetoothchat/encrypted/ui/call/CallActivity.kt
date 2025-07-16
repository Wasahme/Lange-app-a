package com.bluetoothchat.encrypted.ui.call

import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bluetoothchat.encrypted.R
import com.bluetoothchat.encrypted.audio.CallState
import com.bluetoothchat.encrypted.databinding.ActivityCallBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber

@AndroidEntryPoint
class CallActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCallBinding
    private val viewModel: CallViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCallBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        setupObservers()
        viewModel.startCall()
    }

    private fun setupUI() {
        // End call button
        binding.buttonEndCall.setOnClickListener {
            viewModel.endCall()
            finish()
        }

        // Mute button
        binding.buttonMute.setOnClickListener {
            viewModel.toggleMute()
        }

        // Speaker button
        binding.buttonSpeaker.setOnClickListener {
            viewModel.toggleSpeaker()
        }

        // Volume controls
        binding.seekBarVolume.setOnSeekBarChangeListener(object : android.widget.SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: android.widget.SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    viewModel.setVolume(progress / 100f)
                }
            }
            override fun onStartTrackingTouch(seekBar: android.widget.SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: android.widget.SeekBar?) {}
        })
    }

    private fun setupObservers() {
        lifecycleScope.launch {
            // Observe call state
            viewModel.callState.collectLatest { state ->
                updateCallState(state)
            }
        }

        lifecycleScope.launch {
            // Observe call duration
            viewModel.callDuration.collectLatest { duration ->
                updateCallDuration(duration)
            }
        }

        lifecycleScope.launch {
            // Observe mute state
            viewModel.isMuted.collectLatest { isMuted ->
                updateMuteButton(isMuted)
            }
        }

        lifecycleScope.launch {
            // Observe speaker state
            viewModel.isSpeakerOn.collectLatest { isSpeakerOn ->
                updateSpeakerButton(isSpeakerOn)
            }
        }

        lifecycleScope.launch {
            // Observe audio level
            viewModel.audioLevel.collectLatest { level ->
                updateAudioLevel(level)
            }
        }

        lifecycleScope.launch {
            // Observe connected device
            viewModel.connectedDeviceName.collectLatest { deviceName ->
                binding.textViewDeviceName.text = deviceName
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
    }

    private fun updateCallState(state: CallState) {
        when (state) {
            CallState.IDLE -> {
                binding.textViewCallStatus.text = getString(R.string.call_ended)
                binding.buttonEndCall.isEnabled = false
                binding.buttonMute.isEnabled = false
                binding.buttonSpeaker.isEnabled = false
            }
            CallState.STARTING -> {
                binding.textViewCallStatus.text = getString(R.string.calling)
                binding.buttonEndCall.isEnabled = true
                binding.buttonMute.isEnabled = false
                binding.buttonSpeaker.isEnabled = false
            }
            CallState.ACTIVE -> {
                binding.textViewCallStatus.text = getString(R.string.call_connected)
                binding.buttonEndCall.isEnabled = true
                binding.buttonMute.isEnabled = true
                binding.buttonSpeaker.isEnabled = true
            }
            CallState.ENDING -> {
                binding.textViewCallStatus.text = getString(R.string.call_ended)
                binding.buttonEndCall.isEnabled = false
                binding.buttonMute.isEnabled = false
                binding.buttonSpeaker.isEnabled = false
            }
            CallState.ERROR -> {
                binding.textViewCallStatus.text = getString(R.string.call_failed)
                binding.buttonEndCall.isEnabled = true
                binding.buttonMute.isEnabled = false
                binding.buttonSpeaker.isEnabled = false
            }
        }
    }

    private fun updateCallDuration(duration: String) {
        binding.textViewCallDuration.text = duration
    }

    private fun updateMuteButton(isMuted: Boolean) {
        if (isMuted) {
            binding.buttonMute.setIconResource(R.drawable.ic_mic_off)
            binding.buttonMute.text = getString(R.string.unmute)
            binding.buttonMute.setBackgroundColor(getColor(R.color.design_default_color_error))
        } else {
            binding.buttonMute.setIconResource(R.drawable.ic_mic)
            binding.buttonMute.text = getString(R.string.mute)
            binding.buttonMute.setBackgroundColor(getColor(R.color.design_default_color_surface))
        }
    }

    private fun updateSpeakerButton(isSpeakerOn: Boolean) {
        if (isSpeakerOn) {
            binding.buttonSpeaker.setIconResource(R.drawable.ic_volume_up)
            binding.buttonSpeaker.setBackgroundColor(getColor(R.color.design_default_color_primary))
        } else {
            binding.buttonSpeaker.setIconResource(R.drawable.ic_volume_down)
            binding.buttonSpeaker.setBackgroundColor(getColor(R.color.design_default_color_surface))
        }
    }

    private fun updateAudioLevel(level: Float) {
        // Update audio level indicator
        binding.progressBarAudioLevel.progress = (level * 100).toInt()
        
        // Update visual feedback
        val alpha = 0.3f + (level * 0.7f)
        binding.imageViewAudioWave.alpha = alpha
    }

    private fun showError(error: String) {
        Toast.makeText(this, error, Toast.LENGTH_LONG).show()
        Timber.e("Call error: $error")
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.endCall()
    }

    override fun onBackPressed() {
        // Prevent accidental call ending
        viewModel.endCall()
        super.onBackPressed()
    }
}