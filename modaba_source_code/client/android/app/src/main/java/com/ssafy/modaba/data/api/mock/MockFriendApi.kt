package com.ssafy.modaba.data.api.mock

import com.ssafy.modaba.data.TokenManager
import com.ssafy.modaba.data.api.FriendApi
import com.ssafy.modaba.data.model.FriendshipStatus
import com.ssafy.modaba.data.model.response.ProfileSimpleResponse
import retrofit2.Response

class MockFriendApi(
    private val tokenManager: TokenManager
) : FriendApi {
    override suspend fun sendRequest(userId: Long): Response<Unit> {
        val currentUser = getCurrentUser() ?: return mockError(401, "Unauthorized")
        if (currentUser.id == userId) return mockError(409, "Cannot send request to self")
        if (MockDataStore.getUserById(userId) == null) return mockError(404, "User not found")
        if (!MockDataStore.addFriendRequest(currentUser.id, userId)) {
            return mockError(409, "Request already exists")
        }
        return Response.success(202, Unit)
    }

    override suspend fun getFriends(): Response<List<ProfileSimpleResponse>> {
        val currentUser = getCurrentUser() ?: return mockError(401, "Unauthorized")
        val friends = MockDataStore.listFriends(currentUser.id).map { it.toProfileResponse() }
        return Response.success(friends)
    }

    override suspend fun getPending(): Response<List<ProfileSimpleResponse>> {
        val currentUser = getCurrentUser() ?: return mockError(401, "Unauthorized")
        val pending = MockDataStore.listPending(currentUser.id).map { it.toProfileResponse() }
        return Response.success(pending)
    }

    override suspend fun respondRequest(userId: Long, status: FriendshipStatus): Response<Unit> {
        val currentUser = getCurrentUser() ?: return mockError(401, "Unauthorized")
        if (status == FriendshipStatus.PENDING) return mockError(400, "Invalid status")
        val updated = MockDataStore.respondToRequest(userId, currentUser.id, status)
        return if (updated) Response.success(204, Unit) else mockError(404, "Request not found")
    }

    override suspend fun deleteFriend(userId: Long): Response<Unit> {
        val currentUser = getCurrentUser() ?: return mockError(401, "Unauthorized")
        val removed = MockDataStore.removeFriendshipBetween(currentUser.id, userId)
        return if (removed) Response.success(204, Unit) else mockError(404, "Friend not found")
    }

    private fun getCurrentUser(): MockDataStore.MockUser? {
        return MockDataStore.getUserFromToken(tokenManager.getAccessToken(), "access")
    }

    private fun MockDataStore.MockUser.toProfileResponse(): ProfileSimpleResponse {
        return ProfileSimpleResponse(id = id, nickname = nickname, imageLink = imageLink)
    }
}
