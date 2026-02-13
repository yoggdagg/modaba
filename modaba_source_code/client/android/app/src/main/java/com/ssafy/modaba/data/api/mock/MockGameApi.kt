package com.ssafy.modaba.data.api.mock

import com.ssafy.modaba.data.TokenManager
import com.ssafy.modaba.data.api.GameApi
import com.ssafy.modaba.data.model.request.TagRequest
import retrofit2.Response

class MockGameApi(
    private val tokenManager: TokenManager
) : GameApi {
    private val knownTags = setOf("mock-nfc-1", "mock-nfc-2")
    private val readyUsers = mutableSetOf<Long>()

    override suspend fun startGame(): Response<Unit> {
        if (MockDataStore.getUserFromToken(tokenManager.getAccessToken(), "access") == null) {
            return mockError(401, "Unauthorized")
        }
        return Response.success(201, Unit)
    }

    override suspend fun setReady(isReady: Boolean): Response<Unit> {
        val user = MockDataStore.getUserFromToken(tokenManager.getAccessToken(), "access")
            ?: return mockError(401, "Unauthorized")
        if (isReady) {
            readyUsers.add(user.id)
        } else {
            readyUsers.remove(user.id)
        }
        return Response.success(204, Unit)
    }

    override suspend fun tag(request: TagRequest): Response<Unit> {
        if (MockDataStore.getUserFromToken(tokenManager.getAccessToken(), "access") == null) {
            return mockError(401, "Unauthorized")
        }
        if (request.nfc.isBlank() || !knownTags.contains(request.nfc)) {
            return mockError(404, "Tag not found")
        }
        return Response.success(204, Unit)
    }
}
