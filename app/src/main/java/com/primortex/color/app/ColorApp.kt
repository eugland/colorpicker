package com.primortex.color.app

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.toArgb
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Palette
import androidx.compose.ui.graphics.Color
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.primortex.color.screens.CameraScreen
import com.primortex.color.screens.ExploreScreen
import com.primortex.color.screens.PaletteScreen
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.primortex.color.screens.LiveCameraScreen
import com.primortex.color.screens.PhotoPickScreen
import com.primortex.color.service.RecentPicksService
import com.primortex.color.ui.util.randomColorArgb


private object CamRoutes {
    const val HOME = "cam_home"
    const val LIVE = "cam_live"
    const val PHOTO_PICK = "cam_photo_pick"
}

@Composable
fun ColorApp() {
    var tab by remember { mutableStateOf(Tab.Camera) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = tab == Tab.Palette,
                    onClick = { tab = Tab.Palette },
                    icon = { Icon(Icons.Filled.Palette, contentDescription = "Palette") },
                    label = { Text("Palette") }
                )

                NavigationBarItem(
                    selected = tab == Tab.Camera,
                    onClick = { tab = Tab.Camera },
                    icon = { Icon(Icons.Filled.Camera, contentDescription = "Camera") },
                    label = { Text("Camera") }
                )

                NavigationBarItem(
                    selected = tab == Tab.Explore,
                    onClick = { tab = Tab.Explore },
                    icon = { Icon(Icons.Filled.Explore, contentDescription = "Explore") },
                    label = { Text("Explore") }
                )
            }
        }
    ) { inner ->
        when (tab) {
            Tab.Camera -> {
                // ✅ Nested nav graph for camera-only flow
                val nav = rememberNavController()

                NavHost(
                    navController = nav,
                    startDestination = CamRoutes.HOME,
                    modifier = Modifier.padding(inner)
                ) {
                    composable(CamRoutes.HOME) {
                        CameraScreen(
                            innerPadding = PaddingValues(0.dp),
                            onOpenLiveCameraPicker = { nav.navigate(CamRoutes.LIVE) },
                            onPickFromAlbum = {
                                // album picking is handled inside LiveCameraScreen via PhotoPicker button
                                // so we just go there
                                nav.navigate(CamRoutes.LIVE)
                            }
                        )
                    }

                    composable(CamRoutes.LIVE) {
                        LiveCameraScreen(
                            onBack = { nav.popBackStack() },
                            onOpenPhotoPick = { uriString ->
                                val encoded = java.net.URLEncoder.encode(uriString, "UTF-8")
                                nav.navigate("${CamRoutes.PHOTO_PICK}?uri=$encoded")
                            }
                        )
                    }

                    composable(
                        route = "${CamRoutes.PHOTO_PICK}?uri={uri}",
                        arguments = listOf(navArgument("uri") { type = NavType.StringType; defaultValue = "" })
                    ) { backStack ->
                        val uri = java.net.URLDecoder.decode(backStack.arguments?.getString("uri") ?: "", "UTF-8")
                        PhotoPickScreen(
                            photoUri = uri,
                            onBack = { nav.popBackStack() }
                        )
                    }
                }
            }

            Tab.Palette -> PaletteScreen(innerPadding = inner)
            Tab.Explore -> ExploreScreen(innerPadding = inner)
        }
    }
}