package com.ssafy.modaba.data.api.mock

import com.ssafy.modaba.data.TokenManager
import com.ssafy.modaba.data.api.AuthApi
import com.ssafy.modaba.data.model.request.LoginRequest
import com.ssafy.modaba.data.model.request.LogoutRequest
import com.ssafy.modaba.data.model.request.PwForgotRequest
import com.ssafy.modaba.data.model.request.PwResetRequest
import com.ssafy.modaba.data.model.request.SignupRequest
import com.ssafy.modaba.data.model.request.TokenReissueRequest
import com.ssafy.modaba.data.model.response.LoginResponse
import com.ssafy.modaba.data.model.response.TokenReissueResponse
import retrofit2.Response

class MockAuthApi(
    private val tokenManager: TokenManager
) : AuthApi {
    override suspend fun login(request: LoginRequest): Response<LoginResponse> {
        val user = MockDataStore.getUserByEmail(request.email)
            ?: return mockError(401, "Invalid credentials")
        if (user.password != request.password) return mockError(401, "Invalid credentials")
        val accessToken = MockDataStore.buildToken("access", request.email)
        val refreshToken = MockDataStore.buildToken("refresh", request.email)
        return Response.success(LoginResponse(refreshToken, accessToken, user.nickname))
    }

    override suspend fun signup(request: SignupRequest): Response<Unit> {
        if (request.email.isBlank() || request.password.isBlank() || request.nickname.isBlank()) {
            return mockError(400, "Missing fields")
        }
        if (MockDataStore.getUserByEmail(request.email) != null) {
            return mockError(409, "Already exists")
        }
        if (MockDataStore.isNicknameTaken(request.nickname)) {
            return mockError(409, "Nickname already exists")
        }
        MockDataStore.createUser(request.email, request.password, request.nickname)
        return Response.success(201, Unit)
    }

    override suspend fun reissue(request: TokenReissueRequest): Response<TokenReissueResponse> {
        val email = MockDataStore.parseToken(request.refreshToken, "refresh")
            ?: return mockError(401, "Invalid token")
        if (MockDataStore.getUserByEmail(email) == null) return mockError(401, "Invalid token")
        val newAccessToken = MockDataStore.buildToken("access", email)
        val newRefreshToken = MockDataStore.buildToken("refresh", email)
        return Response.success(TokenReissueResponse(newAccessToken, newRefreshToken))
    }

    override suspend fun logOut(request: LogoutRequest): Response<Unit> {
        if (request.refreshToken.isBlank()) {
            return mockError(400, "Invalid token")
        }
        if (MockDataStore.getUserFromToken(tokenManager.getAccessToken(), "access") == null) {
            return mockError(401, "Unauthorized")
        }
        tokenManager.clear()
        return Response.success(204, Unit)
    }

    override suspend fun forgotPw(request: PwForgotRequest): Response<Unit> {
        if (request.email.isBlank()) return mockError(400, "Invalid email")
        MockDataStore.createResetToken(request.email)
        return Response.success(202, Unit)
    }

    override suspend fun resetPw(request: PwResetRequest): Response<Unit> {
        if (request.token.isBlank() || request.password.isBlank()) {
            return mockError(400, "Invalid request")
        }
        val email = MockDataStore.consumeResetToken(request.token) ?: return mockError(400, "Invalid token")
        val user = MockDataStore.getUserByEmail(email) ?: return mockError(400, "Invalid token")
        user.password = request.password
        return Response.success(204, Unit)
    }
}
