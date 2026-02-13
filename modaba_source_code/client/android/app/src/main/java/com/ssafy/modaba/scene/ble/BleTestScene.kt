package com.ssafy.modaba.scene.ble

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import com.ssafy.modaba.ble.BlePermissions
import com.ssafy.modaba.ble.id.BLE_ID_MAX_LENGTH
import com.ssafy.modaba.ble.id.BleIdStore
import com.ssafy.modaba.ble.id.sanitizeBleId
import com.ssafy.modaba.ble.scan.BleDevice
import com.ssafy.modaba.ble.scan.BleScanService
import com.ssafy.modaba.ble.scan.BleScanStore
import com.ssafy.modaba.ble.scan.NEAR_RSSI_THRESHOLD
import com.ssafy.modaba.ble.scan.TARGET_HOLD_MS
import com.ssafy.modaba.ble.scan.TARGET_RSSI_THRESHOLD

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BleTestScene(onBack: () -> Unit) {
    val context = LocalContext.current
    val bluetoothManager = remember {
        context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    }
    val bluetoothAdapter = bluetoothManager.adapter
    val scannerAvailable = remember(bluetoothAdapter) { bluetoothAdapter?.bluetoothLeScanner != null }

    val requiredPermissions = remember { BlePermissions.requiredPermissions() }

    var permissionsGranted by remember {
        mutableStateOf(BlePermissions.hasRequired(context))
    }
    val isScanning by BleScanStore.isScanning.collectAsState()
    val isAdvertising by BleScanStore.isAdvertising.collectAsState()
    val message by BleScanStore.message.collectAsState()
    val devices by BleScanStore.devices.collectAsState()
    val myId by BleScanStore.myId.collectAsState()
    val targetId by BleScanStore.targetId.collectAsState()
    val targetTagged by BleScanStore.targetTagged.collectAsState()
    val targetRssi by BleScanStore.targetRssi.collectAsState()

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        permissionsGranted = result.values.all { it }
    }

    val enableBluetoothLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        // No-op; Bluetooth state is checked directly.
    }

    val canScan = permissionsGranted && bluetoothAdapter?.isEnabled == true && scannerAvailable

    val savedMyId = remember { BleIdStore.getOrCreate(context) }
    var myIdInput by remember { mutableStateOf(savedMyId) }
    LaunchedEffect(savedMyId) {
        BleScanStore.setMyId(savedMyId)
    }

    val deviceList = devices.values.sortedByDescending { it.lastSeen }
    val trimmedTargetId = targetId.trim()
    val filteredList = if (trimmedTargetId.isBlank()) {
        deviceList
    } else {
        deviceList.filter { it.advertisedId == trimmedTargetId }
    }
    val taggedDevices = filteredList.filter { it.rssi >= NEAR_RSSI_THRESHOLD }
    val otherDevices = filteredList.filter { it.rssi < NEAR_RSSI_THRESHOLD }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("BLE 테스트") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                }
            )
        }
    ) { padding ->
        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            StatusCard(
                bluetoothAdapter = bluetoothAdapter,
                permissionsGranted = permissionsGranted,
                isScanning = isScanning,
                isAdvertising = isAdvertising
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { permissionLauncher.launch(requiredPermissions.toTypedArray()) }) {
                    Text("권한 요청")
                }
                TextButton(
                    onClick = {
                        enableBluetoothLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
                    },
                    enabled = bluetoothAdapter?.isEnabled == false
                ) {
                    Text("블루투스 켜기")
                }
                TextButton(onClick = { BleScanStore.clearDevices() }) {
                    Text("목록 초기화")
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = myIdInput,
                        onValueChange = { myIdInput = it },
                        label = { Text("내 ID (광고)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        val sanitizedInput = sanitizeBleId(myIdInput)
                        TextButton(
                            onClick = {
                                val stored = BleIdStore.set(context, myIdInput)
                                if (stored.isBlank()) {
                                    BleScanStore.setMessage("ID는 영문/숫자/[-_]만 가능합니다.")
                                } else {
                                    BleScanStore.setMyId(stored)
                                    myIdInput = stored
                                    if (isScanning) {
                                        BleScanService.refresh(context)
                                    }
                                }
                            },
                            enabled = sanitizedInput.isNotBlank()
                        ) {
                            Text("ID 적용")
                        }
                    }
                    Text(
                        text = "허용 문자: 영문/숫자/-/_ , 최대 ${BLE_ID_MAX_LENGTH}자",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "현재 광고 ID: ${myId.ifBlank { "-" }}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = targetId,
                        onValueChange = { BleScanStore.setTargetId(it) },
                        label = { Text("상대 ID (광고 필터)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = if (trimmedTargetId.isBlank()) {
                            "상대 태깅 상태: 대상 없음"
                        } else {
                            "상대 태깅 상태: ${if (targetTagged) "태깅됨" else "미태깅"}"
                        },
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "상대 RSSI: ${targetRssi?.let { "$it dBm" } ?: "-"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "상대 ID는 BLE 광고 데이터에서 읽습니다.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
                    val packageName = context.packageName
                    val isIgnoringBattery = powerManager.isIgnoringBatteryOptimizations(packageName)
                    Text(
                        text = if (isIgnoringBattery) {
                            "배터리 최적화 제외됨"
                        } else {
                            "배터리 최적화 적용 중"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    TextButton(
                        onClick = {
                            val intent = Intent(
                                android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                            ).apply {
                                data = Uri.parse("package:$packageName")
                            }
                            context.startActivity(intent)
                        }
                    ) {
                        Text("배터리 최적화 제외 요청")
                    }
                }
            }

            Button(
                onClick = {
                    if (!canScan) {
                        BleScanStore.setMessage(
                            when {
                                !permissionsGranted -> "권한이 필요합니다."
                                bluetoothAdapter == null -> "블루투스를 지원하지 않습니다."
                                bluetoothAdapter.isEnabled.not() -> "블루투스가 꺼져 있습니다."
                                else -> "스캔을 시작할 수 없습니다."
                            }
                        )
                        return@Button
                    }
                    if (isScanning) {
                        BleScanService.stop(context)
                    } else {
                        BleScanService.start(context)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isScanning) "스캔 중지" else "스캔 시작")
            }

            Text(
                text = "백그라운드/화면 꺼짐 상태에서도 스캔을 유지합니다.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = "상대 태깅 기준: RSSI >= ${TARGET_RSSI_THRESHOLD} dBm, ${TARGET_HOLD_MS}ms 유지",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "근접 기준: RSSI >= ${NEAR_RSSI_THRESHOLD} dBm (약 10cm)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (isScanning && filteredList.isEmpty()) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            DeviceSection(
                title = "근접 기기 (약 10cm)",
                emptyMessage = "근접 기기가 없습니다.",
                items = taggedDevices
            )

            DeviceSection(
                title = "주변 기기",
                emptyMessage = "주변 기기가 없습니다.",
                items = otherDevices
            )

            message?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
            }

            Spacer(Modifier.weight(1f))
        }
    }
}

@Composable
private fun StatusCard(
    bluetoothAdapter: BluetoothAdapter?,
    permissionsGranted: Boolean,
    isScanning: Boolean,
    isAdvertising: Boolean
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            val bluetoothStatus = when {
                bluetoothAdapter == null -> "블루투스 미지원"
                bluetoothAdapter.isEnabled -> "블루투스 켜짐"
                else -> "블루투스 꺼짐"
            }
            Text(bluetoothStatus, style = MaterialTheme.typography.titleMedium)
            Text(
                if (permissionsGranted) "권한 허용됨" else "권한 필요",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                if (isScanning) "스캔 중" else "스캔 대기",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                if (isAdvertising) "광고 중" else "광고 대기",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun DeviceSection(
    title: String,
    emptyMessage: String,
    items: List<BleDevice>
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        if (items.isEmpty()) {
            Text(emptyMessage, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            items.forEach { device ->
                DeviceRow(device)
            }
        }
    }
}

@Composable
private fun DeviceRow(device: BleDevice) {
    val name = device.name?.ifBlank { null } ?: "알 수 없음"
    val tagState = if (device.rssi >= NEAR_RSSI_THRESHOLD) "근접" else "원거리"
    val idLabel = device.advertisedId ?: "-"
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(name, style = MaterialTheme.typography.titleMedium)
            Text("ID: $idLabel", style = MaterialTheme.typography.bodySmall)
            Text("주소: ${device.address}", style = MaterialTheme.typography.bodySmall)
            Text("RSSI: ${device.rssi} dBm", style = MaterialTheme.typography.bodySmall)
            Text(tagState, style = MaterialTheme.typography.bodySmall)
        }
    }
}
