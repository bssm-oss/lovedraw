package com.example.couplecanvas.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.couplecanvas.presentation.screen.auth.LoginScreen
import com.example.couplecanvas.presentation.screen.home.HomeScreen
import com.example.couplecanvas.presentation.screen.permission.PermissionOnboardingScreen
import com.example.couplecanvas.presentation.screen.permission.hasRequiredStartupPermissions
import com.example.couplecanvas.presentation.screen.room.RoomDashboardScreen
import com.example.couplecanvas.presentation.screen.waiting.WaitingRoomScreen

@Composable
fun CoupleCanvasNavHost(
    launchTarget: AppLaunchTarget? = null,
    onLaunchTargetConsumed: () -> Unit = {},
) {
    val navController = rememberNavController()
    val container = LocalAppContainer.current
    val context = LocalContext.current
    val startDestination = if (hasRequiredStartupPermissions(context)) "login" else "permissions"

    fun navigateToHomeOrLaunchTarget() {
        val target = launchTarget
        if (target != null) {
            navController.navigate(target.route) {
                popUpTo("login") { inclusive = true }
                launchSingleTop = true
            }
            onLaunchTargetConsumed()
        } else {
            navController.navigate("home") {
                popUpTo("login") { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    fun navigateAfterSignIn() {
        if (hasRequiredStartupPermissions(context)) {
            navigateToHomeOrLaunchTarget()
        } else {
            navController.navigate("permissions") {
                popUpTo("login") { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    fun navigateAfterPermissions() {
        if (container.authRepository.currentUser != null) {
            navigateToHomeOrLaunchTarget()
        } else {
            navController.navigate("login") {
                popUpTo("permissions") { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    LaunchedEffect(launchTarget, container.authRepository.currentUser?.uid) {
        val target = launchTarget ?: return@LaunchedEffect
        if (container.authRepository.currentUser != null) {
            if (hasRequiredStartupPermissions(context)) {
                navController.navigate(target.route) { launchSingleTop = true }
                onLaunchTargetConsumed()
            } else {
                navController.navigate("permissions") { launchSingleTop = true }
            }
        }
    }

    NavHost(navController = navController, startDestination = startDestination) {
        composable("login") {
            LoginScreen(
                onSignedIn = {
                    navigateAfterSignIn()
                },
            )
        }
        composable("permissions") {
            PermissionOnboardingScreen(
                onReady = { navigateAfterPermissions() },
            )
        }
        composable("home") {
            HomeScreen(
                onOpenRoom = { roomId -> navController.navigate("room/$roomId") },
                onWaitRoom = { roomId -> navController.navigate("waiting/$roomId") },
                onSignedOut = {
                    navController.navigate("login") {
                        popUpTo("home") { inclusive = true }
                    }
                },
            )
        }
        composable(
            route = "waiting/{roomId}",
            arguments = listOf(navArgument("roomId") { type = NavType.StringType }),
        ) { entry ->
            val roomId = requireNotNull(entry.arguments?.getString("roomId"))
            WaitingRoomScreen(
                roomId = roomId,
                onBack = { navController.popBackStack("home", false) },
                onOpenRoom = {
                    navController.navigate("room/$roomId") {
                        popUpTo("home")
                    }
                },
            )
        }
        composable(
            route = "room/{roomId}?tab={tab}",
            arguments = listOf(
                navArgument("roomId") { type = NavType.StringType },
                navArgument("tab") {
                    type = NavType.IntType
                    defaultValue = 0
                },
            ),
        ) { entry ->
            val roomId = requireNotNull(entry.arguments?.getString("roomId"))
            val initialTab = entry.arguments?.getInt("tab") ?: 0
            RoomDashboardScreen(roomId = roomId, initialTab = initialTab, onBack = { navController.popBackStack("home", false) })
        }
    }
}
