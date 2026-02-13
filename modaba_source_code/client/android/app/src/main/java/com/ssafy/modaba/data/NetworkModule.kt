package com.ssafy.modaba.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.ssafy.modaba.BuildConfig
import com.ssafy.modaba.data.api.AccountApi
import com.ssafy.modaba.data.api.AuthApi
import com.ssafy.modaba.data.api.FriendApi
import com.ssafy.modaba.data.api.GameApi
import com.ssafy.modaba.data.api.mock.MockAccountApi
import com.ssafy.modaba.data.api.mock.MockAuthApi
import com.ssafy.modaba.data.api.mock.MockFriendApi
import com.ssafy.modaba.data.api.mock.MockGameApi
import com.ssafy.modaba.data.api.RoomApi
import com.ssafy.modaba.data.api.RegionApi
import com.ssafy.modaba.data.api.mock.MockRoomApi
import com.ssafy.modaba.data.api.mock.MockRegionApi
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object NetworkModule {
    private const val BASE_URL = "https://c204.duckdns.org"
    private const val PREFS_NAME = "modaba_network"
    private const val KEY_USE_MOCK = "use_mock_api"

    private lateinit var tokenManager: TokenManager
    private lateinit var prefs: SharedPreferences

    private lateinit var realAuthApi: AuthApi
    private lateinit var realAccountApi: AccountApi
    private lateinit var realFriendApi: FriendApi
    private lateinit var realGameApi: GameApi
    private lateinit var realRoomApi: RoomApi
    private lateinit var realRegionApi: RegionApi

    private lateinit var mockAuthApi: AuthApi
    private lateinit var mockAccountApi: AccountApi
    private lateinit var mockFriendApi: FriendApi
    private lateinit var mockGameApi: GameApi
    private lateinit var mockRoomApi: RoomApi
    private lateinit var mockRegionApi: RegionApi

    lateinit var authApi: AuthApi
        private set
    lateinit var accountApi: AccountApi
        private set
    lateinit var friendApi: FriendApi
        private set
    lateinit var gameApi: GameApi
        private set
    lateinit var roomApi: RoomApi
        private set
    lateinit var regionApi: RegionApi
        private set

    fun init(context: Context) {
        tokenManager = TokenManager(context.applicationContext)
        prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        val retrofit = buildRetrofit()
        realAuthApi = retrofit.create(AuthApi::class.java)
        realAccountApi = retrofit.create(AccountApi::class.java)
        realFriendApi = retrofit.create(FriendApi::class.java)
        realGameApi = retrofit.create(GameApi::class.java)
        realRoomApi = retrofit.create(RoomApi::class.java)
        realRegionApi = retrofit.create(RegionApi::class.java)

        mockAuthApi = MockAuthApi(tokenManager)
        mockAccountApi = MockAccountApi(tokenManager)
        mockFriendApi = MockFriendApi(tokenManager)
        mockGameApi = MockGameApi(tokenManager)
        mockRoomApi = MockRoomApi()
        mockRegionApi = MockRegionApi()

        applyMockSetting(isMockEnabled())
    }

    fun getTokenManager(): TokenManager = tokenManager

    fun isMockEnabled(): Boolean = BuildConfig.DEBUG && prefs.getBoolean(KEY_USE_MOCK, false)

    fun setMockEnabled(enabled: Boolean) {
        if (!BuildConfig.DEBUG) return
        prefs.edit { putBoolean(KEY_USE_MOCK, enabled) }
        applyMockSetting(enabled)
    }

    private fun applyMockSetting(enabled: Boolean) {
        authApi = if (enabled) mockAuthApi else realAuthApi
        accountApi = if (enabled) mockAccountApi else realAccountApi
        friendApi = if (enabled) mockFriendApi else realFriendApi
        gameApi = if (enabled) mockGameApi else realGameApi
        roomApi = if (enabled) mockRoomApi else realRoomApi
        regionApi = if (enabled) mockRegionApi else realRegionApi
    }

    private fun buildRetrofit(): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(
                OkHttpClient.Builder()
                    .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY })
                    .addInterceptor(AuthInterceptor(tokenManager))
                    .authenticator(TokenAuthenticator(tokenManager) { realAuthApi })
                    .connectTimeout(10, TimeUnit.SECONDS)
                    .build()
            )
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
}
