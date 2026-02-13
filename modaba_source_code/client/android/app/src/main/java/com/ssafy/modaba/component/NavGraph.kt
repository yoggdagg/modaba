package com.ssafy.modaba.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.ssafy.modaba.scene.auth.LoginScene
import com.ssafy.modaba.scene.auth.SignupScene
import com.ssafy.modaba.scene.home.HomeScene
import com.ssafy.modaba.data.NetworkModule
import com.ssafy.modaba.scene.friend.FriendScene
import com.ssafy.modaba.scene.ble.BleTestScene
import com.ssafy.modaba.scene.gps.GpsTestScene
import com.ssafy.modaba.scene.profile.ProfileScene
import com.ssafy.modaba.scene.room.CreateRoomScene
import com.ssafy.modaba.scene.room.RoomSearchScene

@Composable
fun NavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(navController, Screen.Login.route, modifier) {
        composable(Screen.Login.route) {
            LoginScene(
                onLoginSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onSignupClick = { navController.navigate(Screen.Signup.route) }
            )
        }
        
        composable(Screen.Signup.route) {
            SignupScene(
                onSignupSuccess = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Signup.route) { inclusive = true }
                    }
                },
                onBackClick = { navController.popBackStack() }
            )
        }
        
        composable(Screen.Home.route) {
            HomeScene(
                onProfileClick = { navController.navigate(Screen.Profile.route) },
                onFriendsClick = { navController.navigate(Screen.Friends.route) },
                onBleTestClick = { navController.navigate(Screen.BleTest.route) },
                onGpsTestClick = { navController.navigate(Screen.GpsTest.route) },
                onCreateRoomClick = { navController.navigate(Screen.CreateRoom.route) },
                onRoomSearchClick = { navController.navigate(Screen.RoomSearch.route) }
            )
        }

        composable(Screen.Friends.route) {
            FriendScene(onBack = { navController.popBackStack() })
        }

        composable(Screen.BleTest.route) {
            BleTestScene(onBack = { navController.popBackStack() })
        }

        composable(Screen.GpsTest.route) {
            GpsTestScene(onBack = { navController.popBackStack() })
        }

        composable(Screen.Profile.route) {
            ProfileScene(
                onBack = { navController.popBackStack() },
                onLogout = {
                    NetworkModule.getTokenManager().clear()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.CreateRoom.route) {
            CreateRoomScene(
                onBack = { navController.popBackStack() },
                onCreateSuccess = { navController.popBackStack() }
            )
        }

        composable(Screen.RoomSearch.route) {
            RoomSearchScene(
                onBack = { navController.popBackStack() }
            )
        }

    }
}
