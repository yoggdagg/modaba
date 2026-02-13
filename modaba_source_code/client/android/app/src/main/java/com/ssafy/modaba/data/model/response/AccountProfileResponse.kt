package com.ssafy.modaba.data.model.response

data class AccountProfileResponse(
    val id: Long,
    val email: String,
    val nickname: String,
    val imageLink: String?
)
