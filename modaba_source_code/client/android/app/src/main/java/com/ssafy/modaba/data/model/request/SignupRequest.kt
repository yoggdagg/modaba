package com.ssafy.modaba.data.model.request

data class SignupRequest(
    val email: String,
    val password: String,
    val nickname: String
)
