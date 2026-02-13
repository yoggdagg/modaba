package com.ssafy.modaba.ble.scan

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.ssafy.modaba.R
import com.ssafy.modaba.app.MainActivity
import com.ssafy.modaba.ble.BlePermissions
import com.ssafy.modaba.ble.id.BLE_ID_SERVICE_UUID
import com.ssafy.modaba.ble.id.BleIdStore
import com.ssafy.modaba.ble.id.encodeBleId
import com.ssafy.modaba.ble.id.sanitizeBleId

class BleScanService : Service() {
    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        val manager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        manager.adapter
    }
    private val scanner by lazy { bluetoothAdapter?.bluetoothLeScanner }
    private val advertiser by lazy { bluetoothAdapter?.bluetoothLeAdvertiser }
    private val scanSettings = ScanSettings.Builder()
        .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
        .build()
    private val scanFilters = listOf(
        ScanFilter.Builder().setServiceUuid(BLE_ID_SERVICE_UUID).build()
    )
    private val advertiseSettings = AdvertiseSettings.Builder()
        .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
        .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
        .setConnectable(false)
        .build()
    private var wakeLock: PowerManager.WakeLock? = null
    private var isScanning = false
    private var isAdvertising = false
    private var pendingAdvertiseId: String? = null
    private var currentAdvertiseId: String? = null
    private val scanPendingIntent by lazy { createScanPendingIntent() }

    private val advertiseCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
            isAdvertising = true
            currentAdvertiseId = pendingAdvertiseId
            pendingAdvertiseId = null
            BleScanStore.setAdvertising(true)
        }

        override fun onStartFailure(errorCode: Int) {
            isAdvertising = false
            pendingAdvertiseId = null
            BleScanStore.setAdvertising(false)
            BleScanStore.setMessage("광고 시작 실패: $errorCode")
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        BleScanStore.setMyId(BleIdStore.getOrCreate(this))
        when (intent?.action) {
            ACTION_STOP -> {
                stopScanning()
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_REFRESH -> {
                if (isScanning) startAdvertising() else stopAdvertising()
                return START_STICKY
            }
            else -> {
                if (!BlePermissions.hasRequired(this)) {
                    BleScanStore.setMessage("스캔 권한이 필요합니다.")
                    stopSelf()
                    return START_NOT_STICKY
                }
                startForeground(NOTIFICATION_ID, buildNotification())
                startScanning()
                return START_STICKY
            }
        }
    }

    override fun onDestroy() {
        stopScanning()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startScanning() {
        if (isScanning) return
        BleScanProcessor.reset()
        val scanError = validateScanState()
        if (scanError != null) {
            failStartScan(scanError)
            return
        }

        try {
            acquireWakeLock()
            scanner?.startScan(scanFilters, scanSettings, scanPendingIntent)
            isScanning = true
            BleScanStore.setScanning(true)
            BleScanStore.setMessage(null)
            startAdvertising()
        } catch (_: SecurityException) {
            BleScanStore.setMessage("스캔 권한이 필요합니다.")
            stopSelf()
        }
    }

    private fun stopScanning() {
        if (isScanning) {
            try {
                scanner?.stopScan(scanPendingIntent)
            } catch (_: Exception) {
                // Ignore stop failures.
            }
            isScanning = false
        }
        BleScanStore.setScanning(false)
        BleScanProcessor.reset()
        stopAdvertising()
        releaseWakeLock()
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    @SuppressLint("WakelockTimeout")
    private fun acquireWakeLock() {
        if (wakeLock?.isHeld == true) return
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Modaba:BleScan")
            .apply {
                setReferenceCounted(false)
                acquire()
            }
    }

    private fun releaseWakeLock() {
        wakeLock?.let {
            if (it.isHeld) {
                it.release()
            }
        }
        wakeLock = null
    }

    private fun validateScanState(): String? {
        if (!BlePermissions.hasRequired(this)) return "스캔 권한이 필요합니다."
        val adapter = bluetoothAdapter ?: return "블루투스를 지원하지 않습니다."
        if (!adapter.isEnabled) return "블루투스가 꺼져 있습니다."
        if (scanner == null) return "BLE 스캐너를 사용할 수 없습니다."
        return null
    }

    private fun failStartScan(message: String) {
        BleScanStore.setMessage(message)
        stopSelf()
    }

    private fun createScanPendingIntent(): PendingIntent {
        val intent = Intent(this, BleScanReceiver::class.java)
            .setAction(BleScanReceiver.ACTION_BLE_SCAN)
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or
            if (Build.VERSION.SDK_INT >= 31) PendingIntent.FLAG_MUTABLE else 0
        return PendingIntent.getBroadcast(
            this,
            0,
            intent,
            flags
        )
    }

    private fun startAdvertising() {
        val adapter = bluetoothAdapter ?: return failAdvertise("블루투스를 지원하지 않습니다.")
        if (!adapter.isEnabled) return failAdvertise("블루투스가 꺼져 있습니다.")
        val advertiserInstance = advertiser ?: return failAdvertise("BLE 광고를 지원하지 않습니다.")
        val myId = BleScanStore.myId.value
        val sanitizedId = sanitizeBleId(myId)
        if (sanitizedId.isBlank()) return failAdvertise("내 ID를 설정하세요.")
        if (isAdvertising && sanitizedId == currentAdvertiseId) return

        stopAdvertising()
        val advertiseData = AdvertiseData.Builder()
            .addServiceUuid(BLE_ID_SERVICE_UUID)
            .addServiceData(BLE_ID_SERVICE_UUID, encodeBleId(sanitizedId))
            .setIncludeDeviceName(false)
            .build()

        pendingAdvertiseId = sanitizedId
        try {
            advertiserInstance.startAdvertising(advertiseSettings, advertiseData, advertiseCallback)
        } catch (_: SecurityException) {
            failAdvertise("광고 권한이 필요합니다.")
        }
    }

    private fun stopAdvertising() {
        if (!isAdvertising && pendingAdvertiseId == null) return
        try {
            advertiser?.stopAdvertising(advertiseCallback)
        } catch (_: Exception) {
            // Ignore stop failures.
        }
        isAdvertising = false
        pendingAdvertiseId = null
        BleScanStore.setAdvertising(false)
    }

    private fun failAdvertise(message: String) {
        BleScanStore.setMessage(message)
        stopAdvertising()
    }

    private fun buildNotification(): Notification {
        val openIntent = Intent(this, MainActivity::class.java)
        val openPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val stopIntent = Intent(this, BleScanService::class.java).setAction(ACTION_STOP)
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("BLE 스캔 중")
            .setContentText("10cm 태깅 탐지를 유지합니다.")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(openPendingIntent)
            .setOngoing(true)
            .addAction(
                NotificationCompat.Action.Builder(0, "스캔 중지", stopPendingIntent).build()
            )
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID,
            "BLE 스캔",
            NotificationManager.IMPORTANCE_LOW
        )
        manager.createNotificationChannel(channel)
    }

    companion object {
        private const val CHANNEL_ID = "ble_scan"
        private const val NOTIFICATION_ID = 2001
        private const val ACTION_STOP = "com.ssafy.modaba.ble.scan.ACTION_STOP"
        private const val ACTION_REFRESH = "com.ssafy.modaba.ble.scan.ACTION_REFRESH"

        fun start(context: Context) {
            val intent = Intent(context, BleScanService::class.java)
            ContextCompat.startForegroundService(context, intent)
        }

        fun refresh(context: Context) {
            val intent = Intent(context, BleScanService::class.java).setAction(ACTION_REFRESH)
            context.startService(intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, BleScanService::class.java).setAction(ACTION_STOP)
            context.startService(intent)
        }
    }
}
