package com.ssafy.modaba.scene.friend

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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.ssafy.modaba.data.NetworkModule
import com.ssafy.modaba.data.model.FriendshipStatus
import com.ssafy.modaba.data.model.response.ProfileSimpleResponse
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendScene(onBack: () -> Unit) {
    val friendApi = NetworkModule.friendApi
    val scope = rememberCoroutineScope()

    var friends by remember { mutableStateOf(emptyList<ProfileSimpleResponse>()) }
    var pending by remember { mutableStateOf(emptyList<ProfileSimpleResponse>()) }
    var selectedTab by remember { mutableStateOf(0) }
    var userIdInput by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var actionLoading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }

    suspend fun refreshLists() {
        loading = true
        try {
            val friendsRes = friendApi.getFriends()
            if (friendsRes.isSuccessful) {
                friends = friendsRes.body().orEmpty()
            } else {
                message = "친구 목록 로드 실패: ${friendsRes.code()}"
            }
            val pendingRes = friendApi.getPending()
            if (pendingRes.isSuccessful) {
                pending = pendingRes.body().orEmpty()
            } else {
                message = "요청 목록 로드 실패: ${pendingRes.code()}"
            }
        } catch (e: Exception) {
            message = "네트워크 오류: ${e.message}"
        } finally {
            loading = false
        }
    }

    fun launchAction(label: String, action: suspend () -> String?) {
        scope.launch {
            actionLoading = true
            message = null
            message = try {
                action()
            } catch (e: Exception) {
                "$label 오류: ${e.message}"
            }
            actionLoading = false
        }
    }

    LaunchedEffect(Unit) {
        refreshLists()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("친구") },
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
            OutlinedTextField(
                value = userIdInput,
                onValueChange = { userIdInput = it },
                label = { Text("사용자 ID") },
                enabled = !actionLoading,
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = {
                    launchAction("요청 보내기") {
                        val targetId = userIdInput.trim().toLongOrNull()
                            ?: return@launchAction "사용자 ID를 확인하세요"
                        val res = friendApi.sendRequest(targetId)
                        if (res.isSuccessful) {
                            refreshLists()
                            "요청 전송 완료"
                        } else {
                            "요청 실패: ${res.code()}"
                        }
                    }
                },
                enabled = !actionLoading,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("친구 요청 보내기")
            }

            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("친구") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("요청") }
                )
            }

            if (loading) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                val list = if (selectedTab == 0) friends else pending
                if (list.isEmpty()) {
                    Text("목록이 없습니다", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        list.forEach { profile ->
                            if (selectedTab == 0) {
                                FriendRow(
                                    profile = profile,
                                    enabled = !actionLoading,
                                    onDelete = {
                                        launchAction("친구 삭제") {
                                            val res = friendApi.deleteFriend(profile.id)
                                            if (res.isSuccessful) {
                                                refreshLists()
                                                "친구 삭제 완료"
                                            } else {
                                                "삭제 실패: ${res.code()}"
                                            }
                                        }
                                    }
                                )
                            } else {
                                PendingRow(
                                    profile = profile,
                                    enabled = !actionLoading,
                                    onAccept = {
                                        launchAction("요청 수락") {
                                            val res = friendApi.respondRequest(
                                                profile.id,
                                                FriendshipStatus.ACCEPTED
                                            )
                                            if (res.isSuccessful) {
                                                refreshLists()
                                                "요청 수락 완료"
                                            } else {
                                                "수락 실패: ${res.code()}"
                                            }
                                        }
                                    },
                                    onReject = {
                                        launchAction("요청 거절") {
                                            val res = friendApi.respondRequest(
                                                profile.id,
                                                FriendshipStatus.REJECTED
                                            )
                                            if (res.isSuccessful) {
                                                refreshLists()
                                                "요청 거절 완료"
                                            } else {
                                                "거절 실패: ${res.code()}"
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }

            message?.let {
                Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Spacer(Modifier.weight(1f))
            TextButton(onClick = { scope.launch { refreshLists() } }, enabled = !loading) {
                Text("새로고침")
            }
        }
    }
}

@Composable
private fun FriendRow(
    profile: ProfileSimpleResponse,
    enabled: Boolean,
    onDelete: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(profile.nickname, style = MaterialTheme.typography.titleMedium)
                Text("ID: ${profile.id}", style = MaterialTheme.typography.bodySmall)
            }
            TextButton(onClick = onDelete, enabled = enabled) {
                Text("삭제")
            }
        }
    }
}

@Composable
private fun PendingRow(
    profile: ProfileSimpleResponse,
    enabled: Boolean,
    onAccept: () -> Unit,
    onReject: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Column {
                Text(profile.nickname, style = MaterialTheme.typography.titleMedium)
                Text("ID: ${profile.id}", style = MaterialTheme.typography.bodySmall)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onAccept, enabled = enabled) {
                    Text("수락")
                }
                TextButton(onClick = onReject, enabled = enabled) {
                    Text("거절")
                }
            }
        }
    }
}
