package com.ssafy.modaba.data.model

// API 응답용 데이터 모델
data class RoomDto(
    val roomId: Long,
    val title: String,
    val roomType: String,
    val maxUser: Int,
    val currentUserCount: Int,
    val status: String,
    val appointmentTime: String,
    val placeName: String,
    val regionId: Int? = null,
    val regionName: String? = null
)

data class CreateRoomRequest(
    val title: String,
    val roomType: String,
    val maxUser: Int,
    val appointmentTime: String,
    val placeName: String,
    val targetLat: Double,
    val targetLng: Double,
    val regionId: Int
)

enum class RoomType {
    KYUNGDO,
    APPOINTMENT
}
