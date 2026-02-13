package com.ssafy.modaba.data.api

import com.ssafy.modaba.data.model.request.ProfileUpdateRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH

interface AccountApi {
    @GET("api/v0/accounts")
    suspend fun getProfile(): Response<Unit>

    @PATCH("api/v0/accounts")
    suspend fun updateProfile(@Body request: ProfileUpdateRequest): Response<Unit>

    @PATCH("api/v0/accounts/password")
    suspend fun updatePassword(): Response<Unit>

    @DELETE("api/v0/accounts")
    suspend fun withdraw(): Response<Unit>
}
