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
import androidx.compose.ui.res.stringResource
import com.primortex.color.app.Routes
import com.primortex.color.R
import com.primortex.color.info.InfoContent
import com.primortex.color.screens.CameraScreen
import com.primortex.color.screens.ColorDetailsScreen
import com.primortex.color.screens.ColorSliderScreen
import com.primortex.color.screens.ExploreScreen
import com.primortex.color.screens.InfoDetailScreen
import com.primortex.color.screens.LanguageSelectionScreen
import com.primortex.color.screens.LiveCameraScreen
import com.primortex.color.screens.PaletteScreen
import com.primortex.color.screens.PhotoPickScreen

@Composable
fun ColorApp() {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route.orEmpty()
    val anim = tween<IntOffset>(220)

    val selectedRoot = when {
        route.startsWith(Routes.Tab.PALETTE) -> Routes.Tab.PALETTE
        route.startsWith(Routes.Tab.CAMERA) -> Routes.Tab.CAMERA
        route.startsWith(Routes.Tab.EXPLORE) -> Routes.Tab.EXPLORE
        route.startsWith(Routes.Tool.SLIDER) -> Routes.Tab.CAMERA
        else -> route
    }

    val showBottomBar = route.startsWith("tab/") || route.startsWith(Routes.Tool.SLIDER)

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    NavigationBarItem(
                        selected = selectedRoot == Routes.Tab.PALETTE,
                        onClick = {
                            nav.navigate(Routes.Tab.PALETTE) {
                                popUpTo(Routes.Tab.CAMERA) {
                                    inclusive = false
                                }; launchSingleTop = true
                            }
                        },
                        icon = { Icon(Icons.Filled.Palette, contentDescription = stringResource(R.string.palette)) },
                        label = { Text(stringResource(R.string.palette)) }
                    )
                    NavigationBarItem(
                        selected = selectedRoot == Routes.Tab.CAMERA,
                        onClick = {
                            nav.navigate(Routes.Tab.CAMERA) {
                                popUpTo(Routes.Tab.CAMERA) {
                                    inclusive = true
                                }; launchSingleTop = true
                            }
                        },
                        icon = { Icon(Icons.Filled.Camera, contentDescription = stringResource(R.string.camera)) },
                        label = { Text(stringResource(R.string.camera)) }
                    )
                    NavigationBarItem(
                        selected = selectedRoot == Routes.Tab.EXPLORE,
                        onClick = {
                            nav.navigate(Routes.Tab.EXPLORE) {
                                popUpTo(Routes.Tab.CAMERA) {
                                    inclusive = false
                                }; launchSingleTop = true
                            }
                        },
                        icon = { Icon(Icons.Filled.Explore, contentDescription = stringResource(R.string.explore)) },
                        label = { Text(stringResource(R.string.explore)) }
                    )
                }
            }
        }
    ) { inner ->

        NavHost(
            navController = nav,
            startDestination = Routes.Tab.PALETTE,
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
            composable(Routes.Tab.CAMERA) {
                CameraScreen(
                    innerPadding = inner,
                    onOpenLiveCameraPicker = { nav.navigate(Routes.Camera.LIVE) },
                    onOpenColorSlider = { nav.navigate(Routes.Tool.SLIDER) },
                    onPickFromAlbum = { uriString ->
                        val encoded = java.net.URLEncoder.encode(uriString, "UTF-8")
                        nav.navigate(Routes.Camera.photoPickWith(encoded))
                    }
                )
            }
            composable(Routes.Tab.PALETTE) { PaletteScreen(innerPadding = inner) }
            composable(Routes.Tab.EXPLORE) {
                ExploreScreen(innerPadding = inner, navigator = nav::navigate)
            }
            composable(Routes.Camera.LIVE) { LiveCameraScreen(onBack = { nav.popBackStack() }) }
            composable(
                route = Routes.Camera.PHOTO_PICK_ROUTE,
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
                route = Routes.Detail.COLOR_ROUTE,
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
                        nav.navigate(Routes.Detail.to(pick.argb, pick.name))
                    }
                )
            }

            composable(Routes.Tool.SLIDER) {
                ColorSliderScreen(
                    innerPadding = inner,
                    onBack = { nav.popBackStack() }
                )
            }

            composable(Routes.Info.COPYRIGHT) {
                InfoDetailScreen(
                    title = stringResource(R.string.copyright_notice),
                    innerPadding = inner,
                    onBack = { nav.popBackStack() },
                    sections = InfoContent.copyrightSections
                )
            }

            composable(Routes.Info.PRIVACY) {
                InfoDetailScreen(
                    title = stringResource(R.string.privacy_statement),
                    innerPadding = inner,
                    onBack = { nav.popBackStack() },
                    sections = InfoContent.privacySections
                )
            }

            composable(Routes.Info.USAGE) {
                InfoDetailScreen(
                    title = stringResource(R.string.usage_guide),
                    innerPadding = inner,
                    onBack = { nav.popBackStack() },
                    sections = InfoContent.usageSections
                )
            }

            composable(Routes.Settings.LANGUAGE) {
                LanguageSelectionScreen(
                    innerPadding = inner,
                    onBack = { nav.popBackStack() }
                )
            }
        }
    }
}
