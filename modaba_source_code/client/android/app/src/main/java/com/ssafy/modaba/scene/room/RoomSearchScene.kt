package com.ssafy.modaba.scene.room

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ssafy.modaba.component.CustomButton
import com.ssafy.modaba.data.NetworkModule
import com.ssafy.modaba.data.model.Neighborhood
import com.ssafy.modaba.data.model.RoomDto
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoomSearchScene(onBack: () -> Unit) {
    val scope = rememberCoroutineScope()

    // Region selection state
    var cities by remember { mutableStateOf<List<String>>(emptyList()) }
    var districts by remember { mutableStateOf<List<String>>(emptyList()) }
    var neighborhoods by remember { mutableStateOf<List<Neighborhood>>(emptyList()) }
    var selectedCity by remember { mutableStateOf<String?>(null) }
    var selectedDistrict by remember { mutableStateOf<String?>(null) }
    var selectedNeighborhood by remember { mutableStateOf<Neighborhood?>(null) }

    // Dropdown states
    var cityExpanded by remember { mutableStateOf(false) }
    var districtExpanded by remember { mutableStateOf(false) }
    var neighborhoodExpanded by remember { mutableStateOf(false) }

    // Room list state
    var rooms by remember { mutableStateOf<List<RoomDto>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    // Join dialog state
    var showJoinDialog by remember { mutableStateOf(false) }
    var selectedRoom by remember { mutableStateOf<RoomDto?>(null) }
    var joinLoading by remember { mutableStateOf(false) }

    // Load cities on init
    LaunchedEffect(Unit) {
        try {
            val res = NetworkModule.regionApi.getCities()
            if (res.isSuccessful) {
                cities = res.body()?.data ?: emptyList()
            }
        } catch (_: Exception) {}
    }

    // Load districts when city changes
    LaunchedEffect(selectedCity) {
        selectedCity?.let { city ->
            districts = emptyList()
            neighborhoods = emptyList()
            selectedDistrict = null
            selectedNeighborhood = null
            rooms = emptyList()
            try {
                val res = NetworkModule.regionApi.getDistricts(city)
                if (res.isSuccessful) {
                    districts = res.body()?.data ?: emptyList()
                }
            } catch (_: Exception) {}
        }
    }

    // Load neighborhoods when district changes
    LaunchedEffect(selectedDistrict) {
        val city = selectedCity
        val district = selectedDistrict
        if (city != null && district != null) {
            neighborhoods = emptyList()
            selectedNeighborhood = null
            rooms = emptyList()
            try {
                val res = NetworkModule.regionApi.getNeighborhoods(city, district)
                if (res.isSuccessful) {
                    neighborhoods = res.body()?.data ?: emptyList()
                }
            } catch (_: Exception) {}
        }
    }

    // Search rooms when neighborhood changes
    LaunchedEffect(selectedNeighborhood) {
        val city = selectedCity
        val district = selectedDistrict
        val neighborhood = selectedNeighborhood
        if (city != null && district != null && neighborhood != null) {
            loading = true
            error = null
            try {
                val res = NetworkModule.roomApi.getRooms(city, district, neighborhood.name)
                if (res.isSuccessful) {
                    rooms = res.body() ?: emptyList()
                } else {
                    error = "검색 실패: ${res.code()}"
                }
            } catch (e: Exception) {
                error = "네트워크 오류: ${e.message}"
            }
            loading = false
        }
    }

    // Join room function
    fun joinRoom(room: RoomDto) {
        scope.launch {
            joinLoading = true
            try {
                // TODO: Replace with actual userId from TokenManager
                val userId = 1L
                val res = NetworkModule.roomApi.joinRoom(room.roomId, userId)
                if (res.isSuccessful) {
                    showJoinDialog = false
                    // Refresh room list
                    val city = selectedCity
                    val district = selectedDistrict
                    val neighborhood = selectedNeighborhood
                    if (city != null && district != null && neighborhood != null) {
                        val refreshRes = NetworkModule.roomApi.getRooms(city, district, neighborhood.name)
                        if (refreshRes.isSuccessful) {
                            rooms = refreshRes.body() ?: emptyList()
                        }
                    }
                } else {
                    error = "참가 실패: ${res.code()}"
                }
            } catch (e: Exception) {
                error = "네트워크 오류: ${e.message}"
            }
            joinLoading = false
        }
    }

    // Join confirmation dialog
    if (showJoinDialog && selectedRoom != null) {
        AlertDialog(
            onDismissRequest = { showJoinDialog = false },
            title = { Text("모임 참가") },
            text = {
                Column {
                    Text("\"${selectedRoom!!.title}\"에 참가하시겠습니까?")
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "장소: ${selectedRoom!!.placeName}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        "인원: ${selectedRoom!!.currentUserCount}/${selectedRoom!!.maxUser}명",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { joinRoom(selectedRoom!!) },
                    enabled = !joinLoading
                ) {
                    if (joinLoading) {
                        CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Text("참가")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showJoinDialog = false }) { Text("취소") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("방 검색") },
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Region selection
            Text("지역 선택", style = MaterialTheme.typography.titleMedium)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 시/도
                ExposedDropdownMenuBox(
                    expanded = cityExpanded,
                    onExpandedChange = { cityExpanded = it },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = selectedCity ?: "시/도",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = cityExpanded) },
                        modifier = Modifier.menuAnchor(),
                        textStyle = MaterialTheme.typography.bodySmall
                    )
                    ExposedDropdownMenu(
                        expanded = cityExpanded,
                        onDismissRequest = { cityExpanded = false }
                    ) {
                        cities.forEach { city ->
                            DropdownMenuItem(
                                text = { Text(city, style = MaterialTheme.typography.bodySmall) },
                                onClick = {
                                    selectedCity = city
                                    cityExpanded = false
                                }
                            )
                        }
                    }
                }

                // 시/군/구
                ExposedDropdownMenuBox(
                    expanded = districtExpanded,
                    onExpandedChange = { if (selectedCity != null) districtExpanded = it },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = selectedDistrict ?: "구/군",
                        onValueChange = {},
                        readOnly = true,
                        enabled = selectedCity != null,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = districtExpanded) },
                        modifier = Modifier.menuAnchor(),
                        textStyle = MaterialTheme.typography.bodySmall
                    )
                    ExposedDropdownMenu(
                        expanded = districtExpanded,
                        onDismissRequest = { districtExpanded = false }
                    ) {
                        districts.forEach { district ->
                            DropdownMenuItem(
                                text = { Text(district, style = MaterialTheme.typography.bodySmall) },
                                onClick = {
                                    selectedDistrict = district
                                    districtExpanded = false
                                }
                            )
                        }
                    }
                }

                // 읍/면/동
                ExposedDropdownMenuBox(
                    expanded = neighborhoodExpanded,
                    onExpandedChange = { if (selectedDistrict != null) neighborhoodExpanded = it },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = selectedNeighborhood?.name ?: "동/읍/면",
                        onValueChange = {},
                        readOnly = true,
                        enabled = selectedDistrict != null,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = neighborhoodExpanded) },
                        modifier = Modifier.menuAnchor(),
                        textStyle = MaterialTheme.typography.bodySmall
                    )
                    ExposedDropdownMenu(
                        expanded = neighborhoodExpanded,
                        onDismissRequest = { neighborhoodExpanded = false }
                    ) {
                        neighborhoods.forEach { neighborhood ->
                            DropdownMenuItem(
                                text = { Text(neighborhood.name, style = MaterialTheme.typography.bodySmall) },
                                onClick = {
                                    selectedNeighborhood = neighborhood
                                    neighborhoodExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            HorizontalDivider()

            // Room list
            if (loading) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (error != null) {
                Text(error!!, color = MaterialTheme.colorScheme.error)
            } else if (rooms.isEmpty() && selectedNeighborhood != null) {
                Text("해당 지역에 모임이 없습니다.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(rooms, key = { it.roomId }) { room ->
                        RoomCard(
                            room = room,
                            onClick = {
                                selectedRoom = room
                                showJoinDialog = true
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RoomCard(room: RoomDto, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(room.title, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = room.status,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (room.status == "WAITING") MaterialTheme.colorScheme.primary
                           else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                "게임: ${if (room.roomType == "KYUNGDO") "경찰과 도둑" else "일반 모임"}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "장소: ${room.placeName}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "일시: ${room.appointmentTime}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    room.regionName ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
                Text(
                    "${room.currentUserCount} / ${room.maxUser} 명",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
