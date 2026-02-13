package com.ssafy.modaba.data

import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(private val tokenManager: TokenManager) : Interceptor {
    
    // 토큰이 필요 없는 API 경로
    private val noAuthPaths = listOf(
        "/api/v0/auth/log-in",
        "/api/v0/auth",           // 회원가입
        "/api/v0/auth/forgot-pw",
        "/api/v0/auth/reset-pw"
    )
    
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val path = original.url.encodedPath
        
        // 인증이 필요 없는 API는 그냥 통과
        if (noAuthPaths.any { path.endsWith(it) }) {
            return chain.proceed(original)
        }
        
        // Access Token이 있으면 헤더에 추가
        val token = tokenManager.getAccessToken()
        val request = if (token != null) {
            original.newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        } else {
            original
        }
        
        return chain.proceed(request)
    }
}
