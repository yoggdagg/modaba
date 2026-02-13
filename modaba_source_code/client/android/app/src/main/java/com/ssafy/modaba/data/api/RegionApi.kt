package com.ssafy.modaba.data.api

import com.ssafy.modaba.data.model.Neighborhood
import com.ssafy.modaba.data.model.RegionResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface RegionApi {
    @GET("/api/v0/regions/cities")
    suspend fun getCities(): Response<RegionResponse<List<String>>>

    @GET("/api/v0/regions/districts")
    suspend fun getDistricts(
        @Query("city") city: String
    ): Response<RegionResponse<List<String>>>

    @GET("/api/v0/regions/neighborhoods")
    suspend fun getNeighborhoods(
        @Query("city") city: String,
        @Query("district") district: String
    ): Response<RegionResponse<List<Neighborhood>>>
}
