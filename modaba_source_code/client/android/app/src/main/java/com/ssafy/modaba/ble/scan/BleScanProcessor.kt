package com.ssafy.modaba.ble.scan

import android.bluetooth.le.ScanResult
import android.content.Context
import android.os.Build
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.ssafy.modaba.ble.id.BLE_ID_SERVICE_UUID

object BleScanProcessor {
    private var targetHoldStartMs: Long? = null
    private var targetTagged = false
    private var currentTargetId: String? = null

    fun processResults(context: Context, results: List<ScanResult>) {
        val targetId = BleScanStore.targetId.value
        if (targetId != currentTargetId.orEmpty()) {
            currentTargetId = targetId.ifBlank { null }
            resetTargetState()
        }

        results.forEach { result ->
            val device = result.device ?: return@forEach
            val address = try {
                device.address
            } catch (_: SecurityException) {
                return@forEach
            }
            val name = try {
                device.name
            } catch (_: SecurityException) {
                null
            }
            val advertisedId = extractAdvertisedId(result)
            BleScanStore.updateDevice(
                BleDevice(
                    address = address,
                    name = name,
                    advertisedId = advertisedId,
                    rssi = result.rssi,
                    lastSeen = System.currentTimeMillis()
                )
            )

            if (targetId.isNotBlank() && advertisedId == targetId) {
                handleTargetRssi(context, result.rssi)
            }
        }
    }

    fun handleScanError(errorCode: Int) {
        BleScanStore.setMessage("스캔 실패: $errorCode")
    }

    fun reset() {
        currentTargetId = null
        resetTargetState()
    }

    private fun resetTargetState() {
        targetHoldStartMs = null
        targetTagged = false
        BleScanStore.setTargetTagged(false)
        BleScanStore.setTargetRssi(null)
    }

    private fun extractAdvertisedId(result: ScanResult): String? {
        val record = result.scanRecord ?: return null
        val data = record.getServiceData(BLE_ID_SERVICE_UUID) ?: return null
        return data.toString(Charsets.UTF_8).trim().takeIf { it.isNotBlank() }
    }

    private fun handleTargetRssi(context: Context, rssi: Int) {
        BleScanStore.setTargetRssi(rssi)
        val meetsThreshold = rssi >= TARGET_RSSI_THRESHOLD
        val now = SystemClock.elapsedRealtime()
        if (meetsThreshold) {
            if (targetHoldStartMs == null) {
                targetHoldStartMs = now
            }
            val heldFor = now - (targetHoldStartMs ?: now)
            if (heldFor >= TARGET_HOLD_MS && !targetTagged) {
                targetTagged = true
                BleScanStore.setTargetTagged(true)
                BleScanStore.setMessage("상대가 태깅되었습니다.")
                vibrateTagged(context)
            }
        } else {
            targetHoldStartMs = null
            if (targetTagged) {
                targetTagged = false
                BleScanStore.setTargetTagged(false)
            }
        }
    }

    private fun vibrateTagged(context: Context) {
        val vibrator = if (Build.VERSION.SDK_INT >= 31) {
            val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            manager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        if (!vibrator.hasVibrator()) return
        if (Build.VERSION.SDK_INT >= 26) {
            vibrator.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(200)
        }
    }
}
