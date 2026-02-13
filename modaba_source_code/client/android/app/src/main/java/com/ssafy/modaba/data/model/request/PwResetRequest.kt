package com.ssafy.modaba.data.model.request

data class PwResetRequest(
    val token: String,
    val password: String
)
