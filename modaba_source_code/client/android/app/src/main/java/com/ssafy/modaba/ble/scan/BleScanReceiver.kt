package com.ssafy.modaba.ble.scan

import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanResult
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

class BleScanReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action != null && action != ACTION_BLE_SCAN) return
        val errorCode = intent.getIntExtra(
            BluetoothLeScanner.EXTRA_ERROR_CODE,
            0
        )
        if (errorCode != NO_ERROR) {
            BleScanProcessor.handleScanError(errorCode)
            return
        }

        val results = getScanResults(intent)
        if (results.isNotEmpty()) {
            BleScanProcessor.processResults(context, results)
        }
    }

    private fun getScanResults(intent: Intent): List<ScanResult> {
        val list = if (Build.VERSION.SDK_INT >= 33) {
            intent.getParcelableArrayListExtra(
                BluetoothLeScanner.EXTRA_LIST_SCAN_RESULT,
                ScanResult::class.java
            )
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableArrayListExtra(BluetoothLeScanner.EXTRA_LIST_SCAN_RESULT)
        }
        return list ?: emptyList()
    }

    companion object {
        const val ACTION_BLE_SCAN = "com.ssafy.modaba.ble.scan.ACTION_BLE_SCAN"
        private const val NO_ERROR = 0
    }
}
