package com.ssafy.modaba.scene.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ssafy.modaba.data.NetworkModule
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScene(
    onProfileClick: () -> Unit,
    onFriendsClick: () -> Unit,
    onBleTestClick: () -> Unit,
    onGpsTestClick: () -> Unit,
    onCreateRoomClick: () -> Unit,
    onRoomSearchClick: () -> Unit
) {
    // 소모임 컨셉에 맞춘 데이터 (경찰과 도둑 게임 포함)
    val meetings = listOf(
        Meeting("1", "강남역 경찰과 도둑 한 판!", "경찰과 도둑", "3/10(일) 14:00", 5, 10),
        Meeting("2", "공원에서 뛰실 분 모집", "스터디", "3/12(화) 19:30", 2, 6)
    )
    val myMeetings = listOf(
        Meeting("3", "주말 보드게임 모임", "보드게임", "3/16(토) 13:00", 4, 6)
    )
    val tabTitles = listOf("참가 가능한 모임", "참가 중인 모임")
    var selectedTab by remember { mutableStateOf(0) }
    val activeMeetings = if (selectedTab == 0) meetings else myMeetings
    
    var testResult by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    
    Scaffold(
        topBar = { 
            TopAppBar(
                title = { Text("모임 탐색") },
                actions = {
                    IconButton(onClick = onRoomSearchClick) {
                        Icon(Icons.Default.Search, "방 검색")
                    }
                    IconButton(onClick = onFriendsClick) {
                        Icon(Icons.Default.People, "친구")
                    }
                    IconButton(onClick = onProfileClick) {
                        Icon(Icons.Default.Person, "프로필")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onCreateRoomClick) {
                Icon(Icons.Default.Add, "방 만들기")
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            // 테스트 결과 표시
            testResult?.let {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (it.startsWith("OK")) 
                            MaterialTheme.colorScheme.primaryContainer 
                        else 
                            MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = it,
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onBleTestClick) {
                    Text("BLE 테스트")
                }
                Spacer(Modifier.width(8.dp))
                TextButton(onClick = onGpsTestClick) {
                    Text("GPS 테스트")
                }
                Spacer(Modifier.width(8.dp))
                TextButton(
                    onClick = {
                        scope.launch {
                            loading = true
                            try {
                                val res = NetworkModule.accountApi.getProfile()
                                testResult = if (res.isSuccessful) {
                                    "OK: profile loaded"
                                } else {
                                    "FAIL: ${res.code()}"
                                }
                            } catch (e: Exception) {
                                testResult = "ERROR: ${e.message}"
                            }
                            loading = false
                        }
                    },
                    enabled = !loading
                ) {
                    if (loading) {
                        CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                    }
                    Spacer(Modifier.width(4.dp))
                    Text("내 정보 확인")
                }
            }
            Spacer(Modifier.height(8.dp))

            TabRow(selectedTabIndex = selectedTab) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(activeMeetings, key = { it.id }) { meeting ->
                    MeetingCard(meeting)
                }
            }
        }
    }
}

data class Meeting(
    val id: String, 
    val title: String, 
    val gameType: String, 
    val dateTime: String,
    val currentPlayers: Int, 
    val maxPlayers: Int
)

@Composable
fun MeetingCard(meeting: Meeting) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(meeting.title, style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(4.dp))
            Text(
                text = "일시: ${meeting.dateTime}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = meeting.gameType,
                    color = MaterialTheme.colorScheme.secondary,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "${meeting.currentPlayers} / ${meeting.maxPlayers} 명",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
