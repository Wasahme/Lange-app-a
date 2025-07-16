package com.bluetoothchat.encrypted.ui.advanced

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bluetoothchat.encrypted.ui.theme.BluetoothChatTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AdvancedChatActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val deviceAddress = intent.getStringExtra("device_address") ?: ""
        
        setContent {
            BluetoothChatTheme {
                AdvancedChatScreen(
                    deviceAddress = deviceAddress,
                    onBackPressed = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvancedChatScreen(
    deviceAddress: String,
    onBackPressed: () -> Unit,
    viewModel: AdvancedChatViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var messageText by remember { mutableStateOf("") }
    
    LaunchedEffect(deviceAddress) {
        viewModel.initializeChat(deviceAddress)
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = uiState.deviceName ?: "جهاز غير معروف",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (uiState.isConnected) "متصل" else "غير متصل",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (uiState.isConnected) Color.Green else Color.Red
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "رجوع"
                        )
                    }
                },
                actions = {
                    // Call button
                    IconButton(onClick = { viewModel.startVoiceCall() }) {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = "مكالمة صوتية"
                        )
                    }
                    
                    // More options
                    IconButton(onClick = { /* TODO: Show options */ }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "المزيد"
                        )
                    }
                }
            )
        },
        bottomBar = {
            MessageInputBar(
                message = messageText,
                onMessageChange = { messageText = it },
                onSendMessage = { 
                    viewModel.sendMessage(messageText)
                    messageText = ""
                },
                onVoiceRecord = { viewModel.startVoiceRecording() },
                isRecording = uiState.isRecording
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Connection status
            if (!uiState.isConnected) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "الاتصال منقطع - جاري إعادة المحاولة...",
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
            
            // Messages list
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.messages) { message ->
                    MessageBubble(
                        message = message,
                        isFromMe = message.isFromMe
                    )
                }
            }
        }
    }
}

@Composable
fun MessageInputBar(
    message: String,
    onMessageChange: (String) -> Unit,
    onSendMessage: () -> Unit,
    onVoiceRecord: () -> Unit,
    isRecording: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = message,
                onValueChange = onMessageChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("اكتب رسالة...") },
                maxLines = 3
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            // Voice record button
            IconButton(
                onClick = onVoiceRecord,
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = if (isRecording) 
                        MaterialTheme.colorScheme.error 
                    else 
                        MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                    contentDescription = if (isRecording) "إيقاف التسجيل" else "تسجيل صوتي",
                    tint = Color.White
                )
            }
            
            // Send button
            IconButton(
                onClick = onSendMessage,
                enabled = message.isNotBlank(),
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "إرسال",
                    tint = Color.White
                )
            }
        }
    }
}

@Composable
fun MessageBubble(
    message: ChatMessage,
    isFromMe: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isFromMe) Arrangement.End else Arrangement.Start
    ) {
        Card(
            modifier = Modifier.widthIn(max = 280.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isFromMe) 
                    MaterialTheme.colorScheme.primary 
                else 
                    MaterialTheme.colorScheme.surfaceVariant
            ),
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isFromMe) 16.dp else 4.dp,
                bottomEnd = if (isFromMe) 4.dp else 16.dp
            )
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                Text(
                    text = message.content,
                    color = if (isFromMe) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Row(
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = message.formattedTime,
                        color = if (isFromMe) Color.White.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.bodySmall
                    )
                    
                    if (isFromMe) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = when (message.status) {
                                MessageStatus.SENDING -> Icons.Default.Schedule
                                MessageStatus.SENT -> Icons.Default.Done
                                MessageStatus.DELIVERED -> Icons.Default.DoneAll
                                MessageStatus.FAILED -> Icons.Default.Error
                                else -> Icons.Default.Done
                            },
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = if (message.status == MessageStatus.DELIVERED) Color.Blue else Color.White.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}

// Data classes for chat
data class ChatMessage(
    val id: String,
    val content: String,
    val timestamp: Long,
    val isFromMe: Boolean,
    val status: MessageStatus = MessageStatus.SENT
) {
    val formattedTime: String
        get() = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
            .format(java.util.Date(timestamp))
}

enum class MessageStatus {
    SENDING, SENT, DELIVERED, FAILED, RECEIVED
}