package com.ssafy.modaba.data.model.response

data class TokenReissueResponse(
    val newAccessToken: String,
    val newRefreshToken: String
)
