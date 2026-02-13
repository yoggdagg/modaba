package com.ssafy.modaba.scene.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ssafy.modaba.BuildConfig
import com.ssafy.modaba.component.CustomButton
import com.ssafy.modaba.component.InputField
import com.ssafy.modaba.component.ModabaLogo
import com.ssafy.modaba.data.NetworkModule
import com.ssafy.modaba.data.model.request.LoginRequest
import kotlinx.coroutines.launch

@Composable
fun LoginScene(onLoginSuccess: () -> Unit, onSignupClick: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val testEmail = "test@test.com"
    val testPassword = "test1234"
    val isDebug = BuildConfig.DEBUG
    var mockEnabled by remember { mutableStateOf(NetworkModule.isMockEnabled()) }

    fun doLogin(targetEmail: String, targetPassword: String) {
        if (targetEmail.isBlank() || targetPassword.isBlank()) {
            error = "입력값을 확인하세요"
            return
        }
        scope.launch {
            loading = true
            error = null
            try {
                val res = NetworkModule.authApi.login(LoginRequest(targetEmail, targetPassword))
                if (res.isSuccessful && res.body() != null) {
                    val body = res.body()!!
                    // ✅ 토큰 저장
                    NetworkModule.getTokenManager().saveTokens(
                        body.accessToken,
                        body.refreshToken,
                        body.nickname
                    )
                    onLoginSuccess()
                } else {
                    error = "로그인 실패"
                }
            } catch (e: Exception) {
                error = "네트워크 오류"
            }
            loading = false
        }
    }

    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        ModabaLogo()

        if (isDebug) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Mock API", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = "Return fake responses (debug only)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = mockEnabled,
                        onCheckedChange = { enabled ->
                            mockEnabled = enabled
                            NetworkModule.setMockEnabled(enabled)
                        }
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        Spacer(Modifier.height(32.dp))

        InputField(
            value = email,
            onValueChange = { email = it; error = null },
            label = "이메일",
            leadingIcon = Icons.Default.Email,
            enabled = !loading
        )

        Spacer(Modifier.height(8.dp))

        InputField(
            value = password,
            onValueChange = { password = it; error = null },
            label = "비밀번호",
            leadingIcon = Icons.Default.Lock,
            isPassword = true,
            enabled = !loading
        )

        error?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = MaterialTheme.colorScheme.error)
        }

        Spacer(Modifier.height(16.dp))

        CustomButton(
            text = "로그인",
            onClick = { doLogin(email, password) },
            enabled = !loading,
            loading = loading
        )

        Spacer(Modifier.height(8.dp))

        CustomButton(
            text = "임시 테스트 로그인",
            onClick = {
                email = testEmail
                password = testPassword
                error = null
                if (isDebug && !mockEnabled) {
                    mockEnabled = true
                    NetworkModule.setMockEnabled(true)
                }
                doLogin(testEmail, testPassword)
            },
            enabled = !loading,
            isOutlined = true
        )

        Spacer(Modifier.height(8.dp))

        CustomButton(
            text = "회원가입",
            onClick = onSignupClick,
            enabled = !loading,
            isOutlined = true
        )
    }
}
