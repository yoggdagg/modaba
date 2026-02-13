package com.ssafy.modaba.data.model

// 기본 공통 응답 구조
data class RegionResponse<T>(
    val data: T,
    val success: Boolean
)

// 읍/면/동 세부 정보 모델
data class Neighborhood(
    val regionId: Int,
    val name: String,
    val lat: Double?,
    val lng: Double?
)
