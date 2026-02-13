package com.ssafy.modaba.data.api.mock

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.Response

internal fun <T> mockError(code: Int, message: String): Response<T> {
    val body = """{"message":"$message"}"""
        .toResponseBody("application/json".toMediaType())
    return Response.error(code, body)
}
