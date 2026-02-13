package com.ssafy.modaba.component

sealed class Screen(val route: String) {
    data object Login : Screen("login")
    data object Signup : Screen("signup")
    data object Home : Screen("home")
    data object Friends : Screen("friends")
    data object Profile : Screen("profile")
    data object BleTest : Screen("ble_test")
    data object GpsTest : Screen("gps_test")
    data object CreateRoom : Screen("create_room")
    data object RoomSearch : Screen("room_search")
}
