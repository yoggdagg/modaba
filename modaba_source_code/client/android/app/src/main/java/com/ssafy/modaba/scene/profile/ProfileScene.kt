package com.ssafy.modaba.scene.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ssafy.modaba.component.CustomButton
import com.ssafy.modaba.data.NetworkModule
import com.ssafy.modaba.data.model.request.LogoutRequest
import com.ssafy.modaba.data.model.request.ProfileUpdateRequest
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScene(
    onBack: () -> Unit,
    onLogout: () -> Unit
) {
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var actionMessage by remember { mutableStateOf<String?>(null) }
    var actionLoading by remember { mutableStateOf(false) }
    var nickname by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    val tokenManager = remember { NetworkModule.getTokenManager() }
    val scope = rememberCoroutineScope()

    fun launchAction(label: String, action: suspend () -> String?) {
        scope.launch {
            actionLoading = true
            actionMessage = null
            actionMessage = try {
                action()
            } catch (e: Exception) {
                "$label 오류: ${e.message}"
            }
            actionLoading = false
        }
    }

    LaunchedEffect(Unit) {
        try {
            val res = NetworkModule.accountApi.getProfile()
            if (res.isSuccessful) {
                nickname = tokenManager.getNickname().orEmpty()
                email = ""
            } else {
                error = "프로필 정보를 불러오지 못했습니다."
            }
        } catch (e: Exception) {
            error = "네트워크 오류가 발생했습니다: ${e.message}"
        } finally {
            loading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("프로필") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (loading) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error)
                }

                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        ProfileRow("닉네임", nickname.ifBlank { "-" })
                        ProfileRow("이메일", email.ifBlank { "-" })
                    }
                }

                actionMessage?.let {
                    Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                Spacer(Modifier.weight(1f))
                val actionEnabled = !actionLoading

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    CustomButton(
                        text = "프로필 수정 요청",
                        onClick = {
                            launchAction("프로필 수정") {
                                val res = NetworkModule.accountApi.updateProfile(ProfileUpdateRequest())
                                if (res.isSuccessful) {
                                    "프로필 수정 요청 완료"
                                } else {
                                    "프로필 수정 실패: ${res.code()}"
                                }
                            }
                        },
                        enabled = actionEnabled,
                        loading = actionLoading
                    )

                    CustomButton(
                        text = "비밀번호 변경 요청",
                        onClick = {
                            launchAction("비밀번호 변경") {
                                val res = NetworkModule.accountApi.updatePassword()
                                if (res.isSuccessful) {
                                    "비밀번호 변경 요청 완료"
                                } else {
                                    "비밀번호 변경 실패: ${res.code()}"
                                }
                            }
                        },
                        enabled = actionEnabled,
                        loading = actionLoading
                    )

                    CustomButton(
                        text = "로그아웃",
                        onClick = {
                            launchAction("로그아웃") {
                                val refreshToken = tokenManager.getRefreshToken()
                                if (refreshToken.isNullOrBlank()) {
                                    return@launchAction "로그아웃 실패: refresh token 없음"
                                }
                                val res = NetworkModule.authApi.logOut(LogoutRequest(refreshToken))
                                if (res.isSuccessful || res.code() == 401) {
                                    onLogout()
                                    null
                                } else {
                                    "로그아웃 실패: ${res.code()}"
                                }
                            }
                        },
                        enabled = actionEnabled,
                        isOutlined = true,
                        loading = actionLoading
                    )

                    CustomButton(
                        text = "회원 탈퇴",
                        onClick = {
                            launchAction("회원 탈퇴") {
                                val res = NetworkModule.accountApi.withdraw()
                                if (res.isSuccessful || res.code() == 401) {
                                    tokenManager.clear()
                                    onLogout()
                                    null
                                } else {
                                    "회원 탈퇴 실패: ${res.code()}"
                                }
                            }
                        },
                        enabled = actionEnabled,
                        isOutlined = true,
                        loading = actionLoading
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}
