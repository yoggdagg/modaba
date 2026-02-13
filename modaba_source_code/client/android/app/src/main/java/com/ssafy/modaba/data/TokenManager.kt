package com.ssafy.modaba.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

class TokenManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("modaba_auth", Context.MODE_PRIVATE)
    
    companion object {
        private const val KEY_ACCESS = "access_token"
        private const val KEY_REFRESH = "refresh_token"
        private const val KEY_NICKNAME = "nickname"
    }
    
    fun saveTokens(accessToken: String, refreshToken: String, nickname: String? = null) =
        prefs.edit {
            putString(KEY_ACCESS, accessToken)
            putString(KEY_REFRESH, refreshToken)
            nickname?.let { putString(KEY_NICKNAME, it) }
        }
    
    fun getAccessToken(): String? = prefs.getString(KEY_ACCESS, null)
    fun getRefreshToken(): String? = prefs.getString(KEY_REFRESH, null)
    fun getNickname(): String? = prefs.getString(KEY_NICKNAME, null)
    
    fun isLoggedIn(): Boolean = getAccessToken() != null
    
    fun clear() = prefs.edit { clear() }
}
