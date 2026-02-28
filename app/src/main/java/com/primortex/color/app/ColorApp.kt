package com.primortex.color.app

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
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
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntOffset
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.primortex.color.R
import com.primortex.color.data.enums.SwatchListType
import com.primortex.color.features.camera.CameraScreen
import com.primortex.color.features.colorblind.ColorBlindEnhancerScreen
import com.primortex.color.features.colorslider.ColorSliderRoute
import com.primortex.color.features.colordetails.ColorDetailsRoute
import com.primortex.color.features.crosshair.CrosshairSettingsRoute
import com.primortex.color.features.explore.ExploreDestination
import com.primortex.color.features.explore.ExploreRoute
import com.primortex.color.features.info.InfoDetailScreen
import com.primortex.color.features.languageselection.LanguageSelectionRoute
import com.primortex.color.features.livecamera.LiveCameraRoute
import com.primortex.color.features.palette.PaletteListRoute
import com.primortex.color.features.palette.PaletteRoute
import com.primortex.color.features.palettedetail.PaletteDetailRoute
import com.primortex.color.features.photopick.PhotoPickRoute
import com.primortex.color.features.swatchlist.SwatchListRoute
import com.primortex.color.features.themeselection.ThemeSelectionRoute
import com.primortex.color.i18n.stringResource
import com.primortex.color.info.InfoContent
import com.primortex.color.service.SettingsService
import com.primortex.color.ui.LocalSnackbarController
import com.primortex.color.ui.TestTags
import com.primortex.color.ui.rememberSnackbarController
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

@SuppressLint("NewApi")
@Composable
fun ColorApp(
    settingsService: SettingsService,
    onLanguageChanged: () -> Unit = {},
    onScreenViewed: (String, String) -> Unit
) {
    val nav = rememberNavController()
    val navigator: ColorNavigator = remember(nav) { NavColorNavigator(nav) }
    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route.orEmpty()
    val anim = tween<IntOffset>(220)
    val snackbarController = rememberSnackbarController()

    val selectedRoot = when {
        route.startsWith(Routes.Tab.PALETTE) -> Routes.Tab.PALETTE
        route.startsWith(Routes.Tab.CAMERA) -> Routes.Tab.CAMERA
        route.startsWith(Routes.Tab.EXPLORE) -> Routes.Tab.EXPLORE
        route.startsWith(Routes.Tool.SLIDER) -> Routes.Tab.CAMERA
        else -> route
    }

    val showBottomBar = route.startsWith("tab/") || route.startsWith(Routes.Tool.SLIDER)
    val context = LocalContext.current
    val activityName = remember(context) {
        context.findActivity()?.javaClass?.simpleName ?: "MainActivity"
    }

    LaunchedEffect(nav, activityName) {
        nav.currentBackStackEntryFlow
            .map { it.destination.route }
            .distinctUntilChanged()
            .collect { currentRoute ->
                val screenName = currentRoute?.let { screenNameForRoute(it) }
                if (screenName != null) {
                    onScreenViewed(screenName, activityName)
                }
            }
    }

    CompositionLocalProvider(LocalSnackbarController provides snackbarController) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarController.hostState) },
            bottomBar = {
                if (showBottomBar) {
                    NavigationBar {
                        NavigationBarItem(
                            selected = selectedRoot == Routes.Tab.PALETTE,
                            onClick = { navigator.openPaletteTab() },
                            modifier = Modifier.testTag(TestTags.TAB_PALETTE),
                            icon = {
                                Icon(
                                    Icons.Filled.Palette,
                                    contentDescription = stringResource(R.string.palette)
                                )
                            },

                            label = { Text(stringResource(R.string.palette)) }
                        )
                        NavigationBarItem(
                            selected = selectedRoot == Routes.Tab.CAMERA,
                            onClick = { navigator.openCameraTab() },
                            modifier = Modifier.testTag(TestTags.TAB_CAMERA),
                            icon = {
                                Icon(
                                    Icons.Filled.Camera,
                                    contentDescription = stringResource(R.string.camera)
                                )
                            },
                            label = { Text(stringResource(R.string.camera)) }
                        )
                        NavigationBarItem(
                            selected = selectedRoot == Routes.Tab.EXPLORE,
                            onClick = { navigator.openExploreTab() },
                            modifier = Modifier.testTag(TestTags.TAB_EXPLORE),
                            icon = {
                                Icon(
                                    Icons.Filled.Explore,
                                    contentDescription = stringResource(R.string.explore)
                                )
                            },
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
                        onOpenLiveCameraPicker = { navigator.openLiveCamera() },
                        onOpenColorSlider = { navigator.openColorSlider() },
                        onOpenColorBlindEnhancer = { navigator.openColorBlindEnhancer() },
                        onPickFromAlbum = { uriString -> navigator.openPhotoPick(uriString) }
                    )
                }
                composable(Routes.Tab.PALETTE) {
                    PaletteRoute(
                        innerPadding = inner,
                        onOpenPalette = { paletteId -> navigator.openPaletteDetail(paletteId) },
                        onOpenLiveCameraPicker = { navigator.openLiveCamera() },
                        onOpenRecentColors = { navigator.openRecentColors() },
                        onOpenSavedColors = { navigator.openSavedColors() },
                        onOpenSavedPalettes = { navigator.openSavedPalettes() },
                        onOpenColorDetail = { pick ->
                            navigator.openColorDetail(
                                pick.argb,
                                pick.name
                            )
                        }
                    )
                }
                composable(Routes.Tab.EXPLORE) {
                    ExploreRoute(
                        innerPadding = inner,
                        onNavigate = { destination ->
                            when (destination) {
                                ExploreDestination.LanguageSettings -> navigator.openLanguageSettings()
                                ExploreDestination.ThemeSettings -> navigator.openThemeSettings()
                                ExploreDestination.CrosshairSettings -> navigator.openCrosshairSettings()
                                ExploreDestination.Copyright -> navigator.openInfoCopyright()
                                ExploreDestination.Privacy -> navigator.openInfoPrivacy()
                                ExploreDestination.Terms -> navigator.openInfoTerms()
                                ExploreDestination.Usage -> navigator.openInfoUsage()
                            }
                        }
                    )
                }
                composable(Routes.Camera.LIVE) {
                    LiveCameraRoute(
                        onBack = { navigator.back() },
                        onOpenPalette = { id, edit -> navigator.openPaletteDetail(id, edit) },
                        onOpenColorDetail = { pick ->
                            navigator.openColorDetail(
                                pick.argb,
                                pick.name
                            )
                        }
                    )
                }
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
                    PhotoPickRoute(
                        photoUri = uri,
                        onBack = { navigator.back() },
                        onOpenPalette = { id, edit -> navigator.openPaletteDetail(id, edit) },
                        onOpenColorDetail = { pick ->
                            navigator.openColorDetail(
                                pick.argb,
                                pick.name
                            )
                        }
                    )
                }

                composable(
                    route = Routes.Detail.PALETTE_ROUTE,
                    arguments = listOf(
                        navArgument("id") { type = NavType.StringType; defaultValue = "" },
                        navArgument("edit") { type = NavType.BoolType; defaultValue = false }
                    )
                ) { backStackEntry ->
                    val paletteId = java.net.URLDecoder.decode(
                        backStackEntry.arguments?.getString("id") ?: "",
                        "UTF-8"
                    )
                    val startInEdit = backStackEntry.arguments?.getBoolean("edit") ?: false

                    PaletteDetailRoute(
                        innerPadding = inner,
                        paletteId = paletteId,
                        startInEditMode = startInEdit,
                        onBack = { navigator.back() },
                        onOpenColorDetail = { pick ->
                            navigator.openColorDetail(
                                pick.argb,
                                pick.name
                            )
                        }
                    )
                }

                composable(
                    route = Routes.Detail.COLOR_ROUTE,
                    arguments = listOf(
                        navArgument("argb") { type = NavType.IntType; defaultValue = 0 },
                        navArgument("name") { type = NavType.StringType; defaultValue = "" }
                    )
                ) {
                    ColorDetailsRoute(
                        onBack = { navigator.back() },
                        onOpenPalette = { id, edit -> navigator.openPaletteDetail(id, edit) }
                    )
                }

                composable(Routes.Tool.SLIDER) {
                    ColorSliderRoute(
                        innerPadding = inner,
                        onBack = { navigator.back() },
                        onOpenColorDetail = { pick ->
                            navigator.openColorDetail(
                                pick.argb,
                                pick.name
                            )
                        }
                    )
                }
                composable(Routes.Tool.COLOR_BLIND) {
                    ColorBlindEnhancerScreen(
                        onBack = { navigator.back() }
                    )
                }

                composable(
                    route = Routes.List.SWATCH_ROUTE,
                    arguments = listOf(
                        navArgument("type") { type = NavType.StringType; defaultValue = "recent" }
                    )
                ) { backStackEntry ->
                    val type = when (backStackEntry.arguments?.getString("type")) {
                        "saved" -> SwatchListType.SAVED
                        else -> SwatchListType.RECENT
                    }

                    SwatchListRoute(
                        innerPadding = inner,
                        type = type,
                        onBack = { navigator.back() },
                        onOpenColorDetail = { pick ->
                            navigator.openColorDetail(pick.argb, pick.name)
                        }
                    )
                }

                composable(Routes.List.PALETTE) {
                    PaletteListRoute(
                        innerPadding = inner,
                        onBack = { navigator.back() },
                        onOpenPalette = { paletteId -> navigator.openPaletteDetail(paletteId) }
                    )
                }

                composable(Routes.Info.COPYRIGHT) {
                    InfoDetailScreen(
                        title = stringResource(R.string.copyright_notice),
                        innerPadding = inner,
                        onBack = { navigator.back() },
                        sections = InfoContent.copyrightSections()
                    )
                }

                composable(Routes.Info.PRIVACY) {
                    InfoDetailScreen(
                        title = stringResource(R.string.privacy_statement),
                        innerPadding = inner,
                        onBack = { navigator.back() },
                        sections = InfoContent.privacySections()
                    )
                }

                composable(Routes.Info.TERMS) {
                    InfoDetailScreen(
                        title = stringResource(R.string.terms_of_service),
                        innerPadding = inner,
                        onBack = { navigator.back() },
                        sections = InfoContent.termsSections()
                    )
                }

                composable(Routes.Info.USAGE) {
                    InfoDetailScreen(
                        title = stringResource(R.string.usage_guide),
                        innerPadding = inner,
                        onBack = { navigator.back() },
                        sections = InfoContent.usageSections()
                    )
                }

                composable(Routes.Settings.LANGUAGE) {
                    LanguageSelectionRoute(
                        innerPadding = inner,
                        onBack = { navigator.back() },
                        onLanguageChanged = onLanguageChanged
                    )
                }

                composable(Routes.Settings.THEME) {
                    ThemeSelectionRoute(
                        innerPadding = inner,
                        settingsService = settingsService,
                        onBack = { navigator.back() }
                    )
                }

                composable(Routes.Settings.CROSSHAIR) {
                    CrosshairSettingsRoute(
                        innerPadding = inner,
                        settingsService = settingsService,
                        onBack = { navigator.back() }
                    )
                }
            }
        }
    }
}

private fun screenNameForRoute(route: String): String? {
    val baseRoute = route.substringBefore("?")
    return when (baseRoute) {
        Routes.Tab.PALETTE -> "PaletteScreen"
        Routes.Tab.CAMERA -> "CameraScreen"
        Routes.Tab.EXPLORE -> "ExploreScreen"
        Routes.Camera.LIVE -> "LiveCameraScreen"
        Routes.Camera.PHOTO_PICK_ROUTE.substringBefore("?") -> "PhotoPickScreen"
        Routes.Detail.COLOR -> "ColorDetailsScreen"
        Routes.Detail.PALETTE -> "PaletteDetailScreen"
        Routes.Tool.SLIDER -> "ColorSliderScreen"
        Routes.Tool.COLOR_BLIND -> "ColorBlindEnhancerScreen"
        Routes.List.SWATCH -> "SwatchListScreen"
        Routes.List.PALETTE -> "PaletteListScreen"
        Routes.Info.COPYRIGHT -> "InfoCopyrightScreen"
        Routes.Info.PRIVACY -> "InfoPrivacyScreen"
        Routes.Info.TERMS -> "InfoTermsScreen"
        Routes.Info.USAGE -> "InfoUsageScreen"
        Routes.Settings.LANGUAGE -> "LanguageSelectionScreen"
        Routes.Settings.THEME -> "ThemeSelectionScreen"
        Routes.Settings.CROSSHAIR -> "CrosshairSettingsScreen"
        else -> baseRoute.takeIf { it.isNotBlank() }
    }
}

private fun Context.findActivity(): Activity? {
    var current = this
    while (current is ContextWrapper) {
        if (current is Activity) {
            return current
        }
        current = current.baseContext
    }
    return null
}




