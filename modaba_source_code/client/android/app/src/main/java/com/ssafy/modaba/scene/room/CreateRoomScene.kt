package com.ssafy.modaba.scene.room

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.location.Location
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.kakao.vectormap.*
import com.kakao.vectormap.camera.CameraUpdateFactory
import com.kakao.vectormap.label.Label
import com.kakao.vectormap.label.LabelOptions
import com.kakao.vectormap.label.LabelStyle
import com.ssafy.modaba.component.CustomButton
import com.ssafy.modaba.component.InputField
import com.ssafy.modaba.data.NetworkModule
import com.ssafy.modaba.data.model.CreateRoomRequest
import com.ssafy.modaba.data.model.Neighborhood
import com.ssafy.modaba.data.model.RoomType
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt
import android.location.Geocoder
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateRoomScene(
    onBack: () -> Unit,
    onCreateSuccess: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current

    // Form state
    var title by remember { mutableStateOf("") }
    var selectedRoomType by remember { mutableStateOf(RoomType.KYUNGDO) }
    var placeName by remember { mutableStateOf("") }
    var maxUser by remember { mutableIntStateOf(6) }
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }
    var selectedTime by remember { mutableStateOf<LocalTime?>(null) }

    // Region selection state
    var cities by remember { mutableStateOf<List<String>>(emptyList()) }
    var districts by remember { mutableStateOf<List<String>>(emptyList()) }
    var neighborhoods by remember { mutableStateOf<List<Neighborhood>>(emptyList()) }
    var selectedCity by remember { mutableStateOf<String?>(null) }
    var selectedDistrict by remember { mutableStateOf<String?>(null) }
    var selectedNeighborhood by remember { mutableStateOf<Neighborhood?>(null) }

    // Map state
    var currentLocation by remember { mutableStateOf<Location?>(null) }
    var targetLat by remember { mutableStateOf(35.176) }
    var targetLng by remember { mutableStateOf(126.905) }
    val mapView = remember { MapView(context) }
    var kakaoMap by remember { mutableStateOf<KakaoMap?>(null) }
    var locationLabel by remember { mutableStateOf<Label?>(null) }
    val locationLabelStyle = remember {
        val bitmap = createLocationDotBitmap(context, 16, 0xFFE53935.toInt())
        LabelStyle.from(bitmap)
    }

    // Location permission
    val locationPermissions = remember {
        listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
    }
    var permissionsGranted by remember { mutableStateOf(hasLocationPermission(context)) }
    val locationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        permissionsGranted = result.values.any { it }
    }

    // UI state
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    // Dropdown expanded states
    var cityExpanded by remember { mutableStateOf(false) }
    var districtExpanded by remember { mutableStateOf(false) }
    var neighborhoodExpanded by remember { mutableStateOf(false) }

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
            try {
                val res = NetworkModule.regionApi.getNeighborhoods(city, district)
                if (res.isSuccessful) {
                    neighborhoods = res.body()?.data ?: emptyList()
                }
            } catch (_: Exception) {}
        }
    }

    // Initialize location
    LaunchedEffect(permissionsGranted) {
        if (permissionsGranted) {
            locationClient.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    currentLocation = it
                    // Only set initial location if no neighborhood selected yet
                    if (selectedNeighborhood == null) {
                        targetLat = it.latitude
                        targetLng = it.longitude
                    }
                }
            }
        }
    }

    // Move map when neighborhood is selected
    LaunchedEffect(selectedNeighborhood) {
        val neighborhood = selectedNeighborhood ?: return@LaunchedEffect
        val city = selectedCity ?: return@LaunchedEffect
        val district = selectedDistrict ?: return@LaunchedEffect
        
        // Try to get coordinates from neighborhood data first
        if (neighborhood.lat != null && neighborhood.lng != null) {
            targetLat = neighborhood.lat
            targetLng = neighborhood.lng
        } else {
            // Use Geocoder to find coordinates from address
            try {
                val geocoder = Geocoder(context, Locale.KOREA)
                val address = "$city $district ${neighborhood.name}"
                @Suppress("DEPRECATION")
                val results = geocoder.getFromLocationName(address, 1)
                if (!results.isNullOrEmpty()) {
                    targetLat = results[0].latitude
                    targetLng = results[0].longitude
                }
            } catch (_: Exception) {
                // Geocoding failed, keep current coordinates
            }
        }
        
        // Move camera to new location
        kakaoMap?.let { map ->
            val target = LatLng.from(targetLat, targetLng)
            map.moveCamera(CameraUpdateFactory.newCenterPosition(target, 16))
            // Update or create marker
            locationLabel?.moveTo(target) ?: run {
                locationLabel = map.labelManager?.layer?.addLabel(
                    LabelOptions.from(target).setStyles(locationLabelStyle)
                )
            }
        }
    }

    // Map lifecycle
    LaunchedEffect(mapView) {
        mapView.start(
            object : MapLifeCycleCallback() {
                override fun onMapDestroy() = Unit
                override fun onMapError(error: Exception) {}
            },
            object : KakaoMapReadyCallback() {
                override fun onMapReady(map: KakaoMap) {
                    kakaoMap = map
                    
                    // Set initial camera position
                    val target = LatLng.from(targetLat, targetLng)
                    map.moveCamera(CameraUpdateFactory.newCenterPosition(target, 15))
                    locationLabel = map.labelManager?.layer?.addLabel(
                        LabelOptions.from(target).setStyles(locationLabelStyle)
                    )
                    
                    // Add click listener for setting location
                    map.setOnMapClickListener { _, latLng, _, _ ->
                        targetLat = latLng.latitude
                        targetLng = latLng.longitude
                        // Update marker position
                        locationLabel?.moveTo(latLng) ?: run {
                            locationLabel = map.labelManager?.layer?.addLabel(
                                LabelOptions.from(latLng).setStyles(locationLabelStyle)
                            )
                        }
                    }
                }
            }
        )
    }

    DisposableEffect(lifecycleOwner, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.resume()
                Lifecycle.Event.ON_PAUSE -> mapView.pause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.finish()
        }
    }

    // Submit form
    fun submitForm() {
        if (title.isBlank()) { error = "방 제목을 입력하세요"; return }
        if (selectedNeighborhood == null) { error = "지역을 선택하세요"; return }
        if (placeName.isBlank()) { error = "장소 이름을 입력하세요"; return }
        if (selectedDate == null || selectedTime == null) { error = "날짜와 시간을 선택하세요"; return }

        val appointmentTime = "${selectedDate}T${selectedTime?.format(DateTimeFormatter.ofPattern("HH:mm:ss"))}"
        val request = CreateRoomRequest(
            title = title,
            roomType = selectedRoomType.name,
            maxUser = maxUser,
            appointmentTime = appointmentTime,
            placeName = placeName,
            targetLat = targetLat,
            targetLng = targetLng,
            regionId = selectedNeighborhood!!.regionId
        )

        scope.launch {
            loading = true
            error = null
            try {
                val res = NetworkModule.roomApi.createRoom(request)
                if (res.isSuccessful) {
                    onCreateSuccess()
                } else {
                    error = "방 생성 실패: ${res.code()}"
                }
            } catch (e: Exception) {
                error = "네트워크 오류: ${e.message}"
            }
            loading = false
        }
    }

    // Date Picker Dialog
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        selectedDate = java.time.Instant.ofEpochMilli(millis)
                            .atZone(java.time.ZoneId.systemDefault())
                            .toLocalDate()
                    }
                    showDatePicker = false
                }) { Text("확인") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("취소") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // Time Picker Dialog
    if (showTimePicker) {
        val timePickerState = rememberTimePickerState()
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    selectedTime = LocalTime.of(timePickerState.hour, timePickerState.minute)
                    showTimePicker = false
                }) { Text("확인") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("취소") }
            },
            text = { TimePicker(state = timePickerState) }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("방 만들기") },
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
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 방 제목
            InputField(
                value = title,
                onValueChange = { title = it; error = null },
                label = "방 제목",
                leadingIcon = Icons.Default.Title
            )

            // 게임 종류 선택
            Text("게임 종류", style = MaterialTheme.typography.labelLarge)
            Row(
                modifier = Modifier.selectableGroup(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                RoomType.entries.forEach { type ->
                    Row(
                        modifier = Modifier
                            .selectable(
                                selected = selectedRoomType == type,
                                onClick = { selectedRoomType = type },
                                role = Role.RadioButton
                            )
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedRoomType == type,
                            onClick = null
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(if (type == RoomType.KYUNGDO) "경찰과 도둑" else "일반 모임")
                    }
                }
            }

            // 지역 선택 (3단 드롭다운)
            Text("지역 선택", style = MaterialTheme.typography.labelLarge)
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

            // 장소 이름
            InputField(
                value = placeName,
                onValueChange = { placeName = it; error = null },
                label = "상세 장소 이름 (예: 전남대 정문)",
                leadingIcon = Icons.Default.Place
            )

            // 지도 (카카오맵)
            Text("모임 장소 지도", style = MaterialTheme.typography.labelLarge)
            if (!permissionsGranted) {
                OutlinedButton(onClick = {
                    permissionLauncher.launch(locationPermissions.toTypedArray())
                }) {
                    Text("위치 권한 허용하기")
                }
            }
            Card(modifier = Modifier.fillMaxWidth().height(200.dp)) {
                AndroidView(
                    factory = { mapView },
                    modifier = Modifier.fillMaxSize()
                )
            }
            Text(
                text = "선택된 좌표: ${"%.4f".format(targetLat)}, ${"%.4f".format(targetLng)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "터치하여 위치를 선택하세요",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )

            // 인원 수 설정
            Text("최대 인원", style = MaterialTheme.typography.labelLarge)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                IconButton(
                    onClick = { if (maxUser > 2) maxUser-- },
                    enabled = maxUser > 2
                ) {
                    Icon(Icons.Default.Remove, "감소")
                }
                Text("$maxUser 명", style = MaterialTheme.typography.headlineMedium)
                IconButton(
                    onClick = { if (maxUser < 20) maxUser++ },
                    enabled = maxUser < 20
                ) {
                    Icon(Icons.Default.Add, "증가")
                }
            }

            // 날짜/시간 선택
            Text("약속 일시", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { showDatePicker = true }) {
                    Icon(Icons.Default.CalendarToday, null)
                    Spacer(Modifier.width(8.dp))
                    Text(selectedDate?.toString() ?: "날짜 선택")
                }
                OutlinedButton(onClick = { showTimePicker = true }) {
                    Icon(Icons.Default.Schedule, null)
                    Spacer(Modifier.width(8.dp))
                    Text(selectedTime?.format(DateTimeFormatter.ofPattern("HH:mm")) ?: "시간 선택")
                }
            }

            // 에러 메시지
            error?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
            }

            Spacer(Modifier.height(8.dp))

            // 완료 버튼
            CustomButton(
                text = "방 만들기",
                onClick = { submitForm() },
                enabled = !loading,
                loading = loading
            )
        }
    }
}

private fun hasLocationPermission(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED
}

private fun createLocationDotBitmap(context: Context, sizeDp: Int, color: Int): Bitmap {
    val density = context.resources.displayMetrics.density
    val sizePx = (sizeDp * density).roundToInt().coerceAtLeast(1)
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color }
    val radius = sizePx / 2f
    canvas.drawCircle(radius, radius, radius, paint)
    return bitmap
}
