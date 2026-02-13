package com.ssafy.modaba.data.api.mock

import com.ssafy.modaba.data.api.RegionApi
import com.ssafy.modaba.data.model.Neighborhood
import com.ssafy.modaba.data.model.RegionResponse
import retrofit2.Response

class MockRegionApi : RegionApi {
    override suspend fun getCities(): Response<RegionResponse<List<String>>> {
        val data = listOf("강원특별자치도", "경기도", "광주광역시", "서울특별시")
        val response = RegionResponse(data, true)
        return Response.success(response)
    }

    override suspend fun getDistricts(city: String): Response<RegionResponse<List<String>>> {
        val data = if (city == "광주광역시") {
            listOf("광산구", "남구", "동구", "북구", "서구")
        } else {
            listOf("예시구1", "예시구2")
        }
        val response = RegionResponse(data, true)
        return Response.success(response)
    }

    override suspend fun getNeighborhoods(
        city: String,
        district: String
    ): Response<RegionResponse<List<Neighborhood>>> {
        val data = if (city == "광주광역시" && district == "북구") {
            listOf(
                Neighborhood(960, "용봉동", null, null),
                Neighborhood(974, "매곡동", null, null)
            )
        } else {
            emptyList()
        }
        val response = RegionResponse(data, true)
        return Response.success(response)
    }
}
