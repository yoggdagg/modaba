package com.ssafy.modaba.data.api

import com.ssafy.modaba.data.model.FriendshipStatus
import com.ssafy.modaba.data.model.response.ProfileSimpleResponse
import retrofit2.Response
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface FriendApi {
    @POST("api/v0/friends/{userId}")
    suspend fun sendRequest(@Path("userId") userId: Long): Response<Unit>

    @GET("api/v0/friends")
    suspend fun getFriends(): Response<List<ProfileSimpleResponse>>

    @GET("api/v0/friends/pending")
    suspend fun getPending(): Response<List<ProfileSimpleResponse>>

    @PATCH("api/v0/friends/{userId}")
    suspend fun respondRequest(
        @Path("userId") userId: Long,
        @Query("status") status: FriendshipStatus
    ): Response<Unit>

    @DELETE("api/v0/friends/{userId}")
    suspend fun deleteFriend(@Path("userId") userId: Long): Response<Unit>
}
