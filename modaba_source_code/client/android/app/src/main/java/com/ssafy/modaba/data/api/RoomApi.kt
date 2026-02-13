package com.ssafy.modaba.data.api

import com.ssafy.modaba.data.model.CreateRoomRequest
import com.ssafy.modaba.data.model.RoomDto
import retrofit2.Response
import retrofit2.http.*

interface RoomApi {
    @POST("/api/v0/rooms")
    suspend fun createRoom(@Body request: CreateRoomRequest): Response<Unit>

    @GET("/api/v0/rooms/search")
    suspend fun getRooms(
        @Query("city") city: String,
        @Query("district") district: String,
        @Query("neighborhood") neighborhood: String
    ): Response<List<RoomDto>>

    @POST("/api/v0/rooms/{roomId}")
    suspend fun joinRoom(
        @Path("roomId") roomId: Long,
        @Query("userId") userId: Long
    ): Response<Unit>

    @DELETE("/api/v0/rooms/{roomId}")
    suspend fun leaveRoom(
        @Path("roomId") roomId: Long,
        @Query("userId") userId: Long
    ): Response<Unit>

    @POST("/api/v0/rooms/test-user")
    suspend fun createTestUser(): Response<Long>
}
