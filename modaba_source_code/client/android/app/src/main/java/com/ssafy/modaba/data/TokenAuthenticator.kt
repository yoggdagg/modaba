package com.ssafy.modaba.data

import com.ssafy.modaba.data.model.request.TokenReissueRequest
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route

class TokenAuthenticator(
    private val tokenManager: TokenManager,
    private val authApiProvider: () -> com.ssafy.modaba.data.api.AuthApi
) : Authenticator {
    
    override fun authenticate(route: Route?, response: Response): Request? {
        // 이미 재시도한 요청이면 포기 (무한루프 방지)
        if (response.request.header("X-Retry") != null) {
            tokenManager.clear()
            return null
        }
        
        val refreshToken = tokenManager.getRefreshToken() ?: run {
            tokenManager.clear()
            return null
        }
        
        // 동기적으로 토큰 갱신 요청
        return runBlocking {
            try {
                val result = authApiProvider().reissue(TokenReissueRequest(refreshToken))
                
                if (result.isSuccessful && result.body() != null) {
                    val body = result.body()!!
                    tokenManager.saveTokens(body.newAccessToken, body.newRefreshToken)
                    
                    // 새 토큰으로 원래 요청 재시도
                    response.request.newBuilder()
                        .header("Authorization", "Bearer ${body.newAccessToken}")
                        .header("X-Retry", "true")
                        .build()
                } else {
                    tokenManager.clear()
                    null
                }
            } catch (e: Exception) {
                tokenManager.clear()
                null
            }
        }
    }
}
