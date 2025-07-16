package com.bluetoothchat.encrypted.ui.advanced

import android.animation.ValueAnimator
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bluetoothchat.encrypted.R
import com.bluetoothchat.encrypted.bluetooth.DeviceDiscoveryResult
import com.bluetoothchat.encrypted.bluetooth.NetworkMetrics
import com.bluetoothchat.encrypted.ui.theme.BluetoothChatTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay

/**
 * Advanced main activity with modern Material Design 3 UI
 * Features:
 * - Animated device discovery
 * - Real-time network metrics
 * - Advanced connection management
 * - Beautiful animations and transitions
 * - Adaptive UI based on device capabilities
 * - Multi-language support
 * - Accessibility features
 */
@AndroidEntryPoint
class AdvancedMainActivity : AppCompatActivity() {

    private val viewModel: AdvancedMainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            BluetoothChatTheme {
                AdvancedMainScreen(
                    viewModel = viewModel,
                    onNavigateToChat = { device ->
                        // Navigate to advanced chat
                        val intent = Intent(this, AdvancedChatActivity::class.java)
                        intent.putExtra("device_address", device.address)
                        startActivity(intent)
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvancedMainScreen(
    viewModel: AdvancedMainViewModel = hiltViewModel(),
    onNavigateToChat: (android.bluetooth.BluetoothDevice) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val networkMetrics by viewModel.networkMetrics.collectAsStateWithLifecycle()
    val discoveredDevices by viewModel.discoveredDevices.collectAsStateWithLifecycle()
    
    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabs = listOf("الأجهزة", "الشبكة", "الإعدادات")
    
    Scaffold(
        topBar = {
            AdvancedTopAppBar(
                title = "الدردشة المشفرة",
                networkMetrics = networkMetrics,
                onSettingsClick = { /* TODO */ }
            )
        },
        floatingActionButton = {
            AnimatedFloatingActionButton(
                isScanning = uiState.isScanning,
                onClick = { viewModel.toggleScanning() }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Network Status Card
            AnimatedVisibility(
                visible = networkMetrics.totalConnections > 0,
                enter = slideInVertically() + fadeIn(),
                exit = slideOutVertically() + fadeOut()
            ) {
                NetworkStatusCard(
                    metrics = networkMetrics,
                    modifier = Modifier.padding(16.dp)
                )
            }
            
            // Tab Row
            TabRow(
                selectedTabIndex = selectedTabIndex,
                modifier = Modifier.fillMaxWidth()
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = { Text(title) }
                    )
                }
            }
            
            // Tab Content
            when (selectedTabIndex) {
                0 -> DevicesTab(
                    devices = discoveredDevices,
                    isScanning = uiState.isScanning,
                    onDeviceClick = onNavigateToChat,
                    onConnectClick = { device -> viewModel.connectToDevice(device) }
                )
                1 -> NetworkTab(
                    networkMetrics = networkMetrics,
                    meshTopology = uiState.meshTopology
                )
                2 -> SettingsTab(
                    currentSettings = uiState.settings,
                    onSettingChanged = { setting, value -> viewModel.updateSetting(setting, value) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvancedTopAppBar(
    title: String,
    networkMetrics: NetworkMetrics,
    onSettingsClick: () -> Unit
) {
    TopAppBar(
        title = {
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                AnimatedVisibility(
                    visible = networkMetrics.totalConnections > 0
                ) {
                    Text(
                        text = "${networkMetrics.totalConnections} اتصال نشط",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        actions = {
            // Signal strength indicator
            SignalStrengthIndicator(
                strength = networkMetrics.averageSignalStrength,
                modifier = Modifier.padding(end = 8.dp)
            )
            
            // Settings button
            IconButton(onClick = onSettingsClick) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "الإعدادات"
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface
        )
    )
}

@Composable
fun AnimatedFloatingActionButton(
    isScanning: Boolean,
    onClick: () -> Unit
) {
    val rotation by animateFloatAsState(
        targetValue = if (isScanning) 360f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )
    
    val scale by animateFloatAsState(
        targetValue = if (isScanning) 1.1f else 1f,
        animationSpec = tween(300)
    )
    
    FloatingActionButton(
        onClick = onClick,
        modifier = Modifier
            .scale(scale)
            .rotate(if (isScanning) rotation else 0f),
        containerColor = if (isScanning) 
            MaterialTheme.colorScheme.secondary 
        else 
            MaterialTheme.colorScheme.primary
    ) {
        Icon(
            imageVector = if (isScanning) Icons.Default.Stop else Icons.Default.Search,
            contentDescription = if (isScanning) "إيقاف البحث" else "البحث عن الأجهزة"
        )
    }
}

@Composable
fun NetworkStatusCard(
    metrics: NetworkMetrics,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "حالة الشبكة",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                NetworkQualityIndicator(
                    quality = metrics.networkReliability
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                MetricItem(
                    label = "الاتصالات",
                    value = "${metrics.totalConnections}",
                    icon = Icons.Default.DeviceHub
                )
                
                MetricItem(
                    label = "الزمن",
                    value = "${metrics.averageLatency}ms",
                    icon = Icons.Default.Timer
                )
                
                MetricItem(
                    label = "الإشارة",
                    value = "${metrics.averageSignalStrength}dBm",
                    icon = Icons.Default.SignalWifi4Bar
                )
            }
        }
    }
}

@Composable
fun MetricItem(
    label: String,
    value: String,
    icon: ImageVector
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.size(24.dp)
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
        )
    }
}

@Composable
fun SignalStrengthIndicator(
    strength: Int,
    modifier: Modifier = Modifier
) {
    val bars = when {
        strength > -50 -> 4
        strength > -70 -> 3
        strength > -85 -> 2
        strength > -100 -> 1
        else -> 0
    }
    
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        repeat(4) { index ->
            val isActive = index < bars
            val height = (index + 1) * 4.dp
            
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(height)
                    .background(
                        color = if (isActive) {
                            when (bars) {
                                4 -> Color.Green
                                3 -> Color.Yellow
                                2 -> Color.Orange
                                else -> Color.Red
                            }
                        } else {
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        },
                        shape = RoundedCornerShape(1.dp)
                    )
            )
        }
    }
}

@Composable
fun NetworkQualityIndicator(
    quality: Double
) {
    val color = when {
        quality > 0.8 -> Color.Green
        quality > 0.6 -> Color.Yellow
        quality > 0.4 -> Color.Orange
        else -> Color.Red
    }
    
    val text = when {
        quality > 0.8 -> "ممتاز"
        quality > 0.6 -> "جيد"
        quality > 0.4 -> "متوسط"
        else -> "ضعيف"
    }
    
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color, CircleShape)
        )
        
        Spacer(modifier = Modifier.width(8.dp))
        
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = color
        )
    }
}

@Composable
fun DevicesTab(
    devices: List<DeviceDiscoveryResult.DeviceFound>,
    isScanning: Boolean,
    onDeviceClick: (android.bluetooth.BluetoothDevice) -> Unit,
    onConnectClick: (android.bluetooth.BluetoothDevice) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (isScanning) {
            item {
                ScanningIndicator()
            }
        }
        
        items(devices) { deviceResult ->
            AdvancedDeviceCard(
                device = deviceResult.device,
                signalStrength = deviceResult.signalStrength,
                capabilities = deviceResult.capabilities,
                onDeviceClick = onDeviceClick,
                onConnectClick = onConnectClick
            )
        }
        
        if (devices.isEmpty() && !isScanning) {
            item {
                EmptyDevicesState()
            }
        }
    }
}

@Composable
fun ScanningIndicator() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                strokeWidth = 2.dp
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Text(
                text = "جاري البحث عن الأجهزة...",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
fun EmptyDevicesState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.DevicesOther,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.outline
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "لا توجد أجهزة",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.outline
        )
        
        Text(
            text = "اضغط على زر البحث للعثور على الأجهزة المتاحة",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outline,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun NetworkTab(
    networkMetrics: NetworkMetrics,
    meshTopology: com.bluetoothchat.encrypted.bluetooth.MeshTopology
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            NetworkMetricsCard(networkMetrics)
        }
        
        item {
            MeshTopologyCard(meshTopology)
        }
        
        item {
            NetworkAnalyticsCard(networkMetrics)
        }
    }
}

@Composable
fun SettingsTab(
    currentSettings: AdvancedSettings,
    onSettingChanged: (String, Any) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            EncryptionSettingsCard(
                currentSettings = currentSettings,
                onSettingChanged = onSettingChanged
            )
        }
        
        item {
            AudioSettingsCard(
                currentSettings = currentSettings,
                onSettingChanged = onSettingChanged
            )
        }
        
        item {
            NetworkSettingsCard(
                currentSettings = currentSettings,
                onSettingChanged = onSettingChanged
            )
        }
    }
}

// Placeholder composables for cards
@Composable
fun AdvancedDeviceCard(
    device: android.bluetooth.BluetoothDevice,
    signalStrength: Int,
    capabilities: com.bluetoothchat.encrypted.bluetooth.DeviceCapabilities,
    onDeviceClick: (android.bluetooth.BluetoothDevice) -> Unit,
    onConnectClick: (android.bluetooth.BluetoothDevice) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onDeviceClick(device) }
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = device.name ?: "جهاز غير معروف",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = device.address,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                SignalStrengthIndicator(strength = signalStrength)
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row {
                    if (capabilities.supportsHighQualityAudio) {
                        Icon(
                            imageVector = Icons.Default.HighQuality,
                            contentDescription = "صوت عالي الجودة",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    
                    if (capabilities.supportsMesh) {
                        Icon(
                            imageVector = Icons.Default.Hub,
                            contentDescription = "شبكة mesh",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                
                Button(
                    onClick = { onConnectClick(device) },
                    modifier = Modifier.height(32.dp)
                ) {
                    Text("اتصال")
                }
            }
        }
    }
}

@Composable
fun NetworkMetricsCard(metrics: NetworkMetrics) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "مقاييس الشبكة",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            // Add metrics content
        }
    }
}

@Composable
fun MeshTopologyCard(topology: com.bluetoothchat.encrypted.bluetooth.MeshTopology) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "طوبولوجيا الشبكة",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            // Add topology visualization
        }
    }
}

@Composable
fun NetworkAnalyticsCard(metrics: NetworkMetrics) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "تحليلات الشبكة",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            // Add analytics content
        }
    }
}

@Composable
fun EncryptionSettingsCard(
    currentSettings: AdvancedSettings,
    onSettingChanged: (String, Any) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "إعدادات التشفير",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            // Add encryption settings
        }
    }
}

@Composable
fun AudioSettingsCard(
    currentSettings: AdvancedSettings,
    onSettingChanged: (String, Any) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "إعدادات الصوت",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            // Add audio settings
        }
    }
}

@Composable
fun NetworkSettingsCard(
    currentSettings: AdvancedSettings,
    onSettingChanged: (String, Any) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "إعدادات الشبكة",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            // Add network settings
        }
    }
}

// Data classes
data class AdvancedSettings(
    val encryptionAlgorithm: String = "AES_GCM",
    val audioCodec: String = "OPUS",
    val noiseReduction: Boolean = true,
    val meshNetworking: Boolean = true,
    val voiceBiometrics: Boolean = false,
    val adaptiveQuality: Boolean = true
)