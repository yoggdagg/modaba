package com.ssafy.modaba.app

import android.app.Application
import com.kakao.vectormap.KakaoMapSdk
import com.ssafy.modaba.R
import com.ssafy.modaba.data.NetworkModule

class ModabaApp : Application() {
    override fun onCreate() {
        super.onCreate()
        NetworkModule.init(this)
        KakaoMapSdk.init(this, getString(R.string.kakao_app_key))
    }
}
