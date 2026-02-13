package com.ssafy.modaba.data.api

import com.ssafy.modaba.data.model.request.LoginRequest
import com.ssafy.modaba.data.model.request.LogoutRequest
import com.ssafy.modaba.data.model.request.PwForgotRequest
import com.ssafy.modaba.data.model.request.PwResetRequest
import com.ssafy.modaba.data.model.request.SignupRequest
import com.ssafy.modaba.data.model.request.TokenReissueRequest
import com.ssafy.modaba.data.model.response.LoginResponse
import com.ssafy.modaba.data.model.response.TokenReissueResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.HTTP
import retrofit2.http.PATCH
import retrofit2.http.POST

interface AuthApi {
    @POST("api/v0/auth/log-in")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @POST("api/v0/auth")
    suspend fun signup(@Body request: SignupRequest): Response<Unit>

    @HTTP(method = "POST", path = "api/v0/auth/log-out", hasBody = true)
    suspend fun logOut(@Body request: LogoutRequest): Response<Unit>

    @POST("api/v0/auth/forgot-pw")
    suspend fun forgotPw(@Body request: PwForgotRequest): Response<Unit>

    @PATCH("api/v0/auth/reset-pw")
    suspend fun resetPw(@Body request: PwResetRequest): Response<Unit>

    @POST("api/v0/auth/reissue")
    suspend fun reissue(@Body request: TokenReissueRequest): Response<TokenReissueResponse>
}
