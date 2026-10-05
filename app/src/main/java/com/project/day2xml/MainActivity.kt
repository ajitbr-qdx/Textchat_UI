package com.project.day2xml

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.project.day2xml.databinding.ActivityMainBinding
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var messageAdapter: MessageAdapter
    private val viewModel: ChatViewModel by viewModels()

    private var currentState: ConnectionState = ConnectionState.DISCONNECTED

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        setupSendButton()
        setupConnectToggle()
        observeViewModel()

        // Auto connect on startup
        val initialUrl = binding.etServerUrl.text.toString().trim().ifEmpty { "wss://echo.websocket.org" }
        viewModel.connect(initialUrl)
    }

    private fun setupRecyclerView() {
        messageAdapter = MessageAdapter { message ->
            Toast.makeText(
                this,
                "${message.sender}: ${message.text}",
                Toast.LENGTH_SHORT
            ).show()
        }

        binding.recyclerViewMessages.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = messageAdapter
        }
    }

    private fun setupSendButton() {
        binding.sendButton.setOnClickListener {
            val text = binding.messageInput.text.toString().trim()
            if (text.isEmpty()) return@setOnClickListener

            viewModel.sendMessage(text)
            binding.messageInput.text?.clear()
        }
    }

    private fun setupConnectToggle() {
        binding.btnConnectToggle.setOnClickListener {
            if (currentState == ConnectionState.CONNECTED ||
                currentState == ConnectionState.CONNECTING ||
                currentState == ConnectionState.RECONNECTING
            ) {
                viewModel.disconnect()
            } else {
                val url = binding.etServerUrl.text.toString().trim().ifEmpty { "wss://echo.websocket.org" }
                viewModel.connect(url)
            }
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.connectionState.collect { state ->
                        updateConnectionUi(state)
                    }
                }
                launch {
                    viewModel.messages.collect { messageList ->
                        messageAdapter.submitList(messageList) {
                            if (messageList.isNotEmpty()) {
                                binding.recyclerViewMessages.scrollToPosition(messageList.lastIndex)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun updateConnectionUi(state: ConnectionState) {
        this.currentState = state

        val (statusText, colorHex, buttonText, isSendEnabled) = when (state) {
            ConnectionState.CONNECTED -> Quadruple("CONNECTED", "#2E7D32", "Disconnect", true)
            ConnectionState.CONNECTING -> Quadruple("CONNECTING...", "#F57C00", "Cancel", false)
            ConnectionState.RECONNECTING -> Quadruple("RECONNECTING...", "#E65100", "Cancel", false)
            ConnectionState.DISCONNECTED -> Quadruple("DISCONNECTED", "#757575", "Connect", false)
        }

        binding.tvStatusBadge.text = statusText
        val background = binding.tvStatusBadge.background as? GradientDrawable
            ?: GradientDrawable().apply { cornerRadius = 24f }
        background.setColor(Color.parseColor(colorHex))
        binding.tvStatusBadge.background = background

        binding.btnConnectToggle.text = buttonText
        binding.sendButton.isEnabled = isSendEnabled
    }

    private data class Quadruple<A, B, C, D>(
        val first: A,
        val second: B,
        val third: C,
        val fourth: D
    )
}
