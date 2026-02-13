package com.ssafy.modaba.ble.scan

import com.ssafy.modaba.ble.id.sanitizeBleId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class BleDevice(
    val address: String,
    val name: String?,
    val advertisedId: String?,
    val rssi: Int,
    val lastSeen: Long
)

object BleScanStore {
    private val _devices = MutableStateFlow<Map<String, BleDevice>>(emptyMap())
    val devices: StateFlow<Map<String, BleDevice>> = _devices

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    private val _myId = MutableStateFlow("")
    val myId: StateFlow<String> = _myId

    private val _targetId = MutableStateFlow("")
    val targetId: StateFlow<String> = _targetId

    private val _targetTagged = MutableStateFlow(false)
    val targetTagged: StateFlow<Boolean> = _targetTagged

    private val _targetRssi = MutableStateFlow<Int?>(null)
    val targetRssi: StateFlow<Int?> = _targetRssi

    private val _isAdvertising = MutableStateFlow(false)
    val isAdvertising: StateFlow<Boolean> = _isAdvertising

    fun updateDevice(device: BleDevice) {
        _devices.value = _devices.value + (device.address to device)
    }

    fun clearDevices() {
        _devices.value = emptyMap()
    }

    fun setMyId(value: String) {
        _myId.value = sanitizeBleId(value)
    }

    fun setTargetId(value: String) {
        _targetId.value = sanitizeBleId(value)
        _targetTagged.value = false
        _targetRssi.value = null
    }

    fun setTargetTagged(tagged: Boolean) {
        _targetTagged.value = tagged
    }

    fun setTargetRssi(rssi: Int?) {
        _targetRssi.value = rssi
    }

    fun setScanning(scanning: Boolean) {
        _isScanning.value = scanning
    }

    fun setAdvertising(advertising: Boolean) {
        _isAdvertising.value = advertising
    }

    fun setMessage(message: String?) {
        _message.value = message
    }
}
