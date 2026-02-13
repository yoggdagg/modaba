package com.ssafy.modaba.data.api.mock

import com.ssafy.modaba.data.TokenManager
import com.ssafy.modaba.data.api.AccountApi
import com.ssafy.modaba.data.model.request.ProfileUpdateRequest
import retrofit2.Response

class MockAccountApi(
    private val tokenManager: TokenManager
) : AccountApi {
    override suspend fun getProfile(): Response<Unit> {
        if (MockDataStore.getUserFromToken(tokenManager.getAccessToken(), "access") == null) {
            return mockError(401, "Unauthorized")
        }
        return Response.success(Unit)
    }

    override suspend fun updateProfile(request: ProfileUpdateRequest): Response<Unit> {
        if (MockDataStore.getUserFromToken(tokenManager.getAccessToken(), "access") == null) {
            return mockError(401, "Unauthorized")
        }
        return Response.success(200, Unit)
    }

    override suspend fun updatePassword(): Response<Unit> {
        if (MockDataStore.getUserFromToken(tokenManager.getAccessToken(), "access") == null) {
            return mockError(401, "Unauthorized")
        }
        return Response.success(204, Unit)
    }

    override suspend fun withdraw(): Response<Unit> {
        val user = MockDataStore.getUserFromToken(tokenManager.getAccessToken(), "access")
            ?: return mockError(401, "Unauthorized")
        MockDataStore.removeUser(user)
        tokenManager.clear()
        return Response.success(204, Unit)
    }
}
