package com.primortex.color.app

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.Composable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Palette
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.primortex.color.screens.CameraScreen
import com.primortex.color.screens.ExploreScreen
import com.primortex.color.screens.PaletteScreen
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.primortex.color.screens.LiveCameraScreen
import com.primortex.color.screens.PhotoPickScreen


private object CamRoutes {
    const val HOME = "cam_home"
    const val LIVE = "cam_live"
    const val PHOTO_PICK = "cam_photo_pick"
}
@Composable
fun ColorApp() {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route.orEmpty()
    val anim = tween<IntOffset>(220)

    val showBottomBar = route.startsWith("tab/")

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    NavigationBarItem(
                        selected = route == "tab/palette",
                        onClick = { nav.navigate("tab/palette") { popUpTo("tab/camera") { inclusive = false }; launchSingleTop = true } },
                        icon = { Icon(Icons.Filled.Palette, contentDescription = "Palette") },
                        label = { Text("Palette") }
                    )
                    NavigationBarItem(
                        selected = route == "tab/camera",
                        onClick = { nav.navigate("tab/camera") { popUpTo("tab/camera") { inclusive = true }; launchSingleTop = true } },
                        icon = { Icon(Icons.Filled.Camera, contentDescription = "Camera") },
                        label = { Text("Camera") }
                    )
                    NavigationBarItem(
                        selected = route == "tab/explore",
                        onClick = { nav.navigate("tab/explore") { popUpTo("tab/camera") { inclusive = false }; launchSingleTop = true } },
                        icon = { Icon(Icons.Filled.Explore, contentDescription = "Explore") },
                        label = { Text("Explore") }
                    )
                }
            }
        }
    ) { inner ->

        NavHost(
            navController = nav,
            startDestination = "tab/camera",
            modifier = Modifier.fillMaxSize(),

            enterTransition = {
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = anim
                )
            },
            exitTransition = {
                slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = anim
                )
            },
            popEnterTransition = {
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = anim
                )
            },
            popExitTransition = {
                slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = anim
                )
            }
        ) {
            // ----- Tabs (respect Scaffold padding) -----
            composable("tab/camera") {
                CameraScreen(
                    innerPadding = inner,
                    onOpenLiveCameraPicker = { nav.navigate("cam/live") },
                    onPickFromAlbum = { nav.navigate("cam/live") }
                )
            }
            composable("tab/palette") { PaletteScreen(innerPadding = inner) }
            composable("tab/explore") { ExploreScreen(innerPadding = inner) }

            // ----- Full-screen (NO bottom bar, NO inner padding) -----
            composable("cam/live") {
                LiveCameraScreen(
                    onBack = { nav.popBackStack() }, // ✅ exits to previous level (tab/camera)
                    onOpenPhotoPick = { uriString ->
                        val encoded = java.net.URLEncoder.encode(uriString, "UTF-8")
                        nav.navigate("cam/photoPick?uri=$encoded")
                    }
                )
            }

            composable(
                route = "cam/photoPick?uri={uri}",
                arguments = listOf(navArgument("uri") { type = NavType.StringType; defaultValue = "" })
            ) { backStackEntry ->
                val uri = java.net.URLDecoder.decode(backStackEntry.arguments?.getString("uri") ?: "", "UTF-8")
                PhotoPickScreen(
                    photoUri = uri,
                    onBack = { nav.popBackStack() } // back to live
                )
            }
        }
    }
}
