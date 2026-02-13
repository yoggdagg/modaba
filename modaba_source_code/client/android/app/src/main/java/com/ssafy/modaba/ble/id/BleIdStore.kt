package com.ssafy.modaba.ble.id

import android.content.Context
import kotlin.random.Random

object BleIdStore {
    private const val PREFS_NAME = "ble_test"
    private const val KEY_MY_ID = "my_id"

    fun getOrCreate(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val existing = prefs.getString(KEY_MY_ID, null)
        if (!existing.isNullOrBlank()) return existing
        val newId = "BLE-" + Random.nextInt(1000, 10_000)
        prefs.edit().putString(KEY_MY_ID, newId).apply()
        return newId
    }

    fun set(context: Context, value: String): String {
        val sanitized = sanitizeBleId(value)
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_MY_ID, sanitized)
            .apply()
        return sanitized
    }
}
