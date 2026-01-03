package com.primortex.color.app

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.primortex.color.screens.CameraScreen
import com.primortex.color.screens.ColorDetailsScreen
import com.primortex.color.screens.ExploreScreen
import com.primortex.color.screens.InfoDetailScreen
import com.primortex.color.screens.InfoDetailSection
import com.primortex.color.screens.LiveCameraScreen
import com.primortex.color.screens.PaletteScreen
import com.primortex.color.screens.PhotoPickScreen


private object CamRoutes {
    const val HOME = "cam_home"
    const val LIVE = "cam_live"
    const val PHOTO_PICK = "cam_photo_pick"
}

private object DetailRoutes {
    const val COLOR = "color/details"
    const val COLOR_ROUTE = "color/details?argb={argb}&name={name}"

    fun to(argb: Int, name: String): String {
        val encName = java.net.URLEncoder.encode(name, "UTF-8")
        return "color/details?argb=$argb&name=$encName"
    }
}

private object InfoRoutes {
    const val COPYRIGHT = "info/copyright"
    const val PRIVACY = "info/privacy"
    const val USAGE = "info/usage"
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
                        onClick = {
                            nav.navigate("tab/palette") {
                                popUpTo("tab/camera") {
                                    inclusive = false
                                }; launchSingleTop = true
                            }
                        },
                        icon = { Icon(Icons.Filled.Palette, contentDescription = "Palette") },
                        label = { Text("Palette") }
                    )
                    NavigationBarItem(
                        selected = route == "tab/camera",
                        onClick = {
                            nav.navigate("tab/camera") {
                                popUpTo("tab/camera") {
                                    inclusive = true
                                }; launchSingleTop = true
                            }
                        },
                        icon = { Icon(Icons.Filled.Camera, contentDescription = "Camera") },
                        label = { Text("Camera") }
                    )
                    NavigationBarItem(
                        selected = route == "tab/explore",
                        onClick = {
                            nav.navigate("tab/explore") {
                                popUpTo("tab/camera") {
                                    inclusive = false
                                }; launchSingleTop = true
                            }
                        },
                        icon = { Icon(Icons.Filled.Explore, contentDescription = "Explore") },
                        label = { Text("Explore") }
                    )
                }
            }
        }
    ) { inner ->

        NavHost(
            navController = nav,
            startDestination = "tab/palette",
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
            composable("tab/camera") {
                CameraScreen(
                    innerPadding = inner,
                    onOpenLiveCameraPicker = { nav.navigate("cam/live") },
                    onPickFromAlbum = { uriString ->
                        val encoded = java.net.URLEncoder.encode(uriString, "UTF-8")
                        nav.navigate("cam/photoPick?uri=$encoded")
                    }
                )
            }
            composable("tab/palette") { PaletteScreen(innerPadding = inner) }
            composable("tab/explore") {
                ExploreScreen(
                    innerPadding = inner,
                    onOpenCopyright = { nav.navigate(InfoRoutes.COPYRIGHT) },
                    onOpenPrivacy = { nav.navigate(InfoRoutes.PRIVACY) },
                    onOpenUsageGuide = { nav.navigate(InfoRoutes.USAGE) }
                )
            }
            composable("cam/live") { LiveCameraScreen(onBack = { nav.popBackStack() }) }
            composable(
                route = "cam/photoPick?uri={uri}",
                arguments = listOf(navArgument("uri") {
                    type = NavType.StringType; defaultValue = ""
                })
            ) { backStackEntry ->
                val uri = java.net.URLDecoder.decode(
                    backStackEntry.arguments?.getString("uri") ?: "",
                    "UTF-8"
                )
                PhotoPickScreen(
                    photoUri = uri,
                    onBack = { nav.popBackStack() },
                )
            }

            composable(
                route = DetailRoutes.COLOR_ROUTE,
                arguments = listOf(
                    navArgument("argb") { type = NavType.IntType; defaultValue = 0 },
                    navArgument("name") { type = NavType.StringType; defaultValue = "" }
                )
            ) { backStackEntry ->
                val argb = backStackEntry.arguments?.getInt("argb") ?: 0
                val name = java.net.URLDecoder.decode(
                    backStackEntry.arguments?.getString("name") ?: "",
                    "UTF-8"
                )

                ColorDetailsScreen(
                    argb = argb,
                    nameHint = name,
                    onBack = { nav.popBackStack() },
                    onOpenColorDetail = { pick ->
                        nav.navigate(DetailRoutes.to(pick.argb, pick.name))
                    }
                )
            }

            composable(InfoRoutes.COPYRIGHT) {
                InfoDetailScreen(
                    title = "Copyright notice",
                    innerPadding = inner,
                    onBack = { nav.popBackStack() },
                    sections = listOf(
                        InfoDetailSection(
                            heading = "Ownership",
                            paragraphs = listOf(
                                "© 2024 Color Picker by Primortex. All rights reserved. The Color Picker name, logo, and application assets are proprietary and may not be reused without permission."
                            )
                        ),
                        InfoDetailSection(
                            heading = "Third-party components",
                            paragraphs = listOf(
                                "Color Picker is built on open-source technologies. We acknowledge and respect the licenses of each library we rely on."
                            ),
                            bullets = listOf(
                                "Jetpack Compose Material 3 for UI components",
                                "CameraX for camera integration",
                                "Coil for image loading",
                                "Ktor for lightweight networking",
                                "Navigation Compose and Accompanist Navigation Animation for in-app navigation"
                            )
                        ),
                        InfoDetailSection(
                            heading = "License notes",
                            paragraphs = listOf(
                                "All third-party libraries remain the property of their respective owners. Their original licenses are preserved and credited here so you can review them in detail."
                            )
                        )
                    )
                )
            }

            composable(InfoRoutes.PRIVACY) {
                InfoDetailScreen(
                    title = "Privacy statement",
                    innerPadding = inner,
                    onBack = { nav.popBackStack() },
                    sections = listOf(
                        InfoDetailSection(
                            heading = "On-device processing",
                            paragraphs = listOf(
                                "Color sampling happens locally. Camera previews and picked photos are only analyzed to extract color values and are not transmitted to servers."
                            )
                        ),
                        InfoDetailSection(
                            heading = "Network usage",
                            paragraphs = listOf(
                                "Network requests are limited to fetching optional supporting data such as palette names. No personal images or camera frames are uploaded."
                            ),
                            bullets = listOf(
                                "If you prefer a fully offline experience, you can revoke network access at the system level.",
                                "You may revoke camera and photo permissions anytime from your device settings."
                            )
                        ),
                        InfoDetailSection(
                            heading = "Data retention",
                            paragraphs = listOf(
                                "Saved colors and palettes stay on your device until you delete them. Clearing the app data or uninstalling removes your saved items."
                            )
                        )
                    )
                )
            }

            composable(InfoRoutes.USAGE) {
                InfoDetailScreen(
                    title = "Usage guide",
                    innerPadding = inner,
                    onBack = { nav.popBackStack() },
                    sections = listOf(
                        InfoDetailSection(
                            heading = "Quick index",
                            paragraphs = listOf("Jump to the task you need."),
                            bullets = listOf(
                                "Pick a color from Live Camera",
                                "Pick a color from a photo",
                                "Save a swatch to your palette",
                                "Compare and copy color values",
                                "Organize palettes"
                            )
                        ),
                        InfoDetailSection(
                            heading = "Pick a color from Live Camera",
                            paragraphs = listOf(
                                "Open Live Camera from the Explore or Camera tab. Aim the crosshair at the target, pinch to zoom for precision, then tap the swatch preview to lock it in."
                            ),
                            bullets = listOf(
                                "Adjust crosshair size and shape in Explore > Settings for better accuracy.",
                                "Use a steady grip or tripod for low-light scenes."
                            )
                        ),
                        InfoDetailSection(
                            heading = "Pick a color from a photo",
                            paragraphs = listOf(
                                "Choose Photo Pick, select an image from your gallery, then tap anywhere on the photo to sample the color at that point."
                            ),
                            bullets = listOf(
                                "Zoom into the photo before tapping to isolate small details.",
                                "Use back to return without saving if you just want to inspect a color."
                            )
                        ),
                        InfoDetailSection(
                            heading = "Save and reuse swatches",
                            paragraphs = listOf(
                                "After capturing a color, tap Save to store it in your palette. Each saved swatch records HEX, RGB, and HSL values for copying."),
                            bullets = listOf(
                                "Tap any saved swatch to copy its HEX code or open details for more formats.",
                                "Long-press or use edit actions (where available) to remove colors you no longer need."
                            )
                        ),
                        InfoDetailSection(
                            heading = "Organize palettes",
                            paragraphs = listOf(
                                "Open the Palette tab to group related swatches. Rearrange or remove items to keep your palette focused on the project at hand."
                            )
                        )
                    )
                )
            }
        }
    }
}
