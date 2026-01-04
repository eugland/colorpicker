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
import com.primortex.color.screens.LanguageSelectionScreen
import com.primortex.color.info.InfoContent
import com.primortex.color.screens.InfoDetailScreen
import com.primortex.color.screens.LiveCameraScreen
import com.primortex.color.screens.PaletteScreen
import com.primortex.color.screens.PhotoPickScreen
import com.primortex.color.screens.ColorSliderScreen

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

private object SliderRoutes {
    const val SLIDER = "tool/slider"
}

private object SettingsRoutes {
    const val LANGUAGE = "settings/language"
}

@Composable
fun ColorApp() {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route.orEmpty()
    val anim = tween<IntOffset>(220)

    val selectedRoot = when {
        route.startsWith("tab/palette") -> "tab/palette"
        route.startsWith("tab/camera") -> "tab/camera"
        route.startsWith("tab/explore") -> "tab/explore"
        route.startsWith(SliderRoutes.SLIDER) -> "tab/camera"
        else -> route
    }

    val showBottomBar = route.startsWith("tab/") || route.startsWith(SliderRoutes.SLIDER)

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    NavigationBarItem(
                        selected = selectedRoot == "tab/palette",
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
                        selected = selectedRoot == "tab/camera",
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
                        selected = selectedRoot == "tab/explore",
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
                    onOpenColorSlider = { nav.navigate(SliderRoutes.SLIDER) },
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
                    onOpenLanguage = { nav.navigate(SettingsRoutes.LANGUAGE) },
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

            composable(SliderRoutes.SLIDER) {
                ColorSliderScreen(
                    innerPadding = inner,
                    onBack = { nav.popBackStack() }
                )
            }

            composable(InfoRoutes.COPYRIGHT) {
                InfoDetailScreen(
                    title = "Copyright notice",
                    innerPadding = inner,
                    onBack = { nav.popBackStack() },
                    sections = InfoContent.copyrightSections
                )
            }

            composable(InfoRoutes.PRIVACY) {
                InfoDetailScreen(
                    title = "Privacy statement",
                    innerPadding = inner,
                    onBack = { nav.popBackStack() },
                    sections = InfoContent.privacySections
                )
            }

            composable(InfoRoutes.USAGE) {
                InfoDetailScreen(
                    title = "Usage guide",
                    innerPadding = inner,
                    onBack = { nav.popBackStack() },
                    sections = InfoContent.usageSections
                )
            }

            composable(SettingsRoutes.LANGUAGE) {
                LanguageSelectionScreen(
                    innerPadding = inner,
                    onBack = { nav.popBackStack() }
                )
            }
        }
    }
}
