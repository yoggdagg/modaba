package com.ssafy.modaba.ble.id

import android.os.ParcelUuid
import java.util.UUID

const val BLE_ID_MAX_LENGTH = 12
val BLE_ID_SERVICE_UUID: ParcelUuid =
    ParcelUuid(UUID.fromString("0000c204-0000-1000-8000-00805f9b34fb"))

fun sanitizeBleId(input: String): String {
    val trimmed = input.trim()
    val filtered = buildString(trimmed.length) {
        for (ch in trimmed) {
            if (
                (ch in 'A'..'Z') ||
                (ch in 'a'..'z') ||
                (ch in '0'..'9') ||
                ch == '-' ||
                ch == '_'
            ) {
                append(ch)
            }
        }
    }
    return if (filtered.length > BLE_ID_MAX_LENGTH) {
        filtered.substring(0, BLE_ID_MAX_LENGTH)
    } else {
        filtered
    }
}

fun encodeBleId(id: String): ByteArray {
    return sanitizeBleId(id).toByteArray(Charsets.UTF_8)
}
