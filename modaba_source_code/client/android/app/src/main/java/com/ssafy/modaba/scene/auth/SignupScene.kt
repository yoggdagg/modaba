package com.ssafy.modaba.scene.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ssafy.modaba.component.CustomButton
import com.ssafy.modaba.component.InputField
import com.ssafy.modaba.data.NetworkModule
import com.ssafy.modaba.data.model.request.SignupRequest
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignupScene(onSignupSuccess: () -> Unit, onBackClick: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var nickname by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPw by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("회원가입") },
                navigationIcon = { IconButton(onClick = onBackClick) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } }
            )
        }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            InputField(
                value = email,
                onValueChange = { email = it; error = null },
                label = "이메일",
                leadingIcon = Icons.Default.Email,
                enabled = !loading
            )
            Spacer(Modifier.height(8.dp))

            InputField(
                value = nickname,
                onValueChange = { nickname = it; error = null },
                label = "닉네임",
                leadingIcon = Icons.Default.Person,
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
            Spacer(Modifier.height(8.dp))

            InputField(
                value = confirmPw,
                onValueChange = { confirmPw = it; error = null },
                label = "비밀번호 확인",
                leadingIcon = Icons.Default.Lock,
                isPassword = true,
                enabled = !loading,
                isError = confirmPw.isNotEmpty() && password != confirmPw
            )

            error?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, color = MaterialTheme.colorScheme.error)
            }

            Spacer(Modifier.height(24.dp))

            CustomButton(
                text = "회원가입",
                onClick = {
                    when {
                        email.isBlank() || nickname.isBlank() || password.isBlank() -> error = "모든 필드를 입력하세요"
                        password != confirmPw -> error = "비밀번호 불일치"
                        password.length < 4 -> error = "비밀번호 4자 이상"
                        else -> scope.launch {
                            loading = true
                            try {
                                val res = NetworkModule.authApi.signup(SignupRequest(email, password, nickname))
                                if (res.isSuccessful) onSignupSuccess() else error = "가입 실패 (${res.code()})"
                            } catch (e: Exception) {
                                error = "네트워크 오류"
                            }
                            loading = false
                        }
                    }
                },
                enabled = !loading,
                loading = loading
            )
        }
    }
}
