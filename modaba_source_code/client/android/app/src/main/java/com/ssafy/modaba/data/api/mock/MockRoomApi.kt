package com.ssafy.modaba.data.api.mock

import com.ssafy.modaba.data.api.RoomApi
import com.ssafy.modaba.data.model.CreateRoomRequest
import com.ssafy.modaba.data.model.RoomDto
import retrofit2.Response

class MockRoomApi : RoomApi {

    private val mockRooms = listOf(
        RoomDto(
            roomId = 15,
            title = "광주 북구 용봉동 모임",
            roomType = "KYUNGDO",
            maxUser = 6,
            currentUserCount = 0,
            status = "WAITING",
            appointmentTime = "2026-02-01T14:00:00",
            placeName = "전남대 정문",
            regionId = 960,
            regionName = "광주광역시 북구 용봉동"
        )
    )

    override suspend fun createRoom(request: CreateRoomRequest): Response<Unit> {
        // Mock successful creation
        return Response.success(201, Unit)
    }

    override suspend fun getRooms(
        city: String,
        district: String,
        neighborhood: String
    ): Response<List<RoomDto>> {
        return Response.success(mockRooms)
    }

    override suspend fun joinRoom(roomId: Long, userId: Long): Response<Unit> {
        return Response.success(200, Unit)
    }

    override suspend fun leaveRoom(roomId: Long, userId: Long): Response<Unit> {
        return Response.success(200, Unit)
    }

    override suspend fun createTestUser(): Response<Long> {
        return Response.success(1L)
    }
}
