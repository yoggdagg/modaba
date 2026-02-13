package com.ssafy.modaba.data.model.response

data class LoginResponse(
    val refreshToken: String,
    val accessToken: String,
    val nickname: String
)
