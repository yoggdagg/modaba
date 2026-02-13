package com.ssafy.modaba.data.api

import com.ssafy.modaba.data.model.request.TagRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Query

interface GameApi {
    @POST("api/v0/games")
    suspend fun startGame(): Response<Unit>

    @PATCH("api/v0/games")
    suspend fun setReady(@Query("isReady") isReady: Boolean): Response<Unit>

    @PATCH("api/v0/games/tagging")
    suspend fun tag(@Body request: TagRequest): Response<Unit>
}
