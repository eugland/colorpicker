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
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.primortex.color.R
import com.primortex.color.info.InfoContent
import com.primortex.color.info.InfoContentService
import com.primortex.color.info.InfoPage
import com.primortex.color.screens.CameraScreen
import com.primortex.color.screens.ColorBlindEnhancerScreen
import com.primortex.color.screens.ColorDetailsScreen
import com.primortex.color.screens.ColorSliderScreen
import com.primortex.color.screens.ExploreScreen
import com.primortex.color.screens.InfoDetailScreen
import com.primortex.color.screens.InfoDetailSection
import com.primortex.color.screens.LanguageSelectionScreen
import com.primortex.color.screens.LiveCameraScreen
import com.primortex.color.screens.IpLookupScreen
import com.primortex.color.screens.PaletteDetailScreen
import com.primortex.color.screens.PaletteScreen
import com.primortex.color.screens.PhotoPickScreen
import com.primortex.color.ui.LocalSnackbarService
import com.primortex.color.ui.rememberSnackbarService

@Composable
fun ColorApp(onLanguageChanged: () -> Unit = {}) {
    val nav = rememberNavController()
    val navigator: ColorNavigator = remember(nav) { NavColorNavigator(nav) }
    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route.orEmpty()
    val anim = tween<IntOffset>(220)
    val snackbarService = rememberSnackbarService()

    val selectedRoot = when {
        route.startsWith(Routes.Tab.PALETTE) -> Routes.Tab.PALETTE
        route.startsWith(Routes.Tab.CAMERA) -> Routes.Tab.CAMERA
        route.startsWith(Routes.Tab.EXPLORE) -> Routes.Tab.EXPLORE
        route.startsWith(Routes.Tool.SLIDER) -> Routes.Tab.CAMERA
        route.startsWith(Routes.Tool.IP_LOOKUP) -> Routes.Tab.CAMERA
        else -> route
    }

    val showBottomBar = route.startsWith("tab/") ||
            route.startsWith(Routes.Tool.SLIDER) ||
            route.startsWith(Routes.Tool.IP_LOOKUP)

    CompositionLocalProvider(LocalSnackbarService provides snackbarService) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarService.hostState) },
            bottomBar = {
                if (showBottomBar) {
                    NavigationBar {
                        NavigationBarItem(
                            selected = selectedRoot == Routes.Tab.PALETTE,
                            onClick = { navigator.openPaletteTab() },
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
                        onOpenIpLookup = { navigator.openIpLookup() },
                        onPickFromAlbum = { uriString -> navigator.openPhotoPick(uriString) }
                    )
                }
                composable(Routes.Tab.PALETTE) {
                    PaletteScreen(
                        innerPadding = inner,
                        onOpenPalette = { palette -> navigator.openPaletteDetail(palette.id) }
                    )
                }
                composable(Routes.Tab.EXPLORE) {
                    ExploreRoute(innerPadding = inner, navigator = navigator)
                }
                composable(Routes.Camera.LIVE) {
                    LiveCameraScreen(
                        onBack = { navigator.back() },
                        onOpenPalette = { id, edit -> navigator.openPaletteDetail(id, edit) }
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
                    PhotoPickScreen(
                        photoUri = uri,
                        onBack = { navigator.back() },
                        onOpenPalette = { id, edit -> navigator.openPaletteDetail(id, edit) }
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

                    PaletteDetailScreen(
                        innerPadding = inner,
                        paletteId = paletteId,
                        startInEditMode = startInEdit,
                        onBack = { navigator.back() },
                        onOpenColorDetail = { pick -> navigator.openColorDetail(pick.argb, pick.name) }
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
                        onBack = { navigator.back() },
                        onOpenColorDetail = { pick -> navigator.openColorDetail(pick.argb, pick.name) }
                    )
                }

                composable(Routes.Tool.SLIDER) {
                    ColorSliderScreen(
                        innerPadding = inner,
                        onBack = { navigator.back() }
                    )
                }
                composable(Routes.Tool.COLOR_BLIND) {
                    ColorBlindEnhancerScreen(
                        onBack = { navigator.back() }
                    )
                }
                composable(Routes.Tool.IP_LOOKUP) {
                    IpLookupScreen(
                        innerPadding = inner,
                        onBack = { navigator.back() }
                    )
                }

                composable(Routes.Info.COPYRIGHT) {
                    val sections = rememberInfoSections(
                        page = InfoPage.COPYRIGHT,
                        fallback = InfoContent.copyrightSections
                    )

                    InfoDetailScreen(
                        title = stringResource(R.string.copyright_notice),
                        innerPadding = inner,
                        onBack = { navigator.back() },
                        sections = sections
                    )
                }

                composable(Routes.Info.PRIVACY) {
                    val sections = rememberInfoSections(
                        page = InfoPage.PRIVACY,
                        fallback = InfoContent.privacySections
                    )

                    InfoDetailScreen(
                        title = stringResource(R.string.privacy_statement),
                        innerPadding = inner,
                        onBack = { navigator.back() },
                        sections = sections
                    )
                }

                composable(Routes.Info.TERMS) {
                    val sections = rememberInfoSections(
                        page = InfoPage.TERMS,
                        fallback = InfoContent.termsSections
                    )

                    InfoDetailScreen(
                        title = stringResource(R.string.terms_of_service),
                        innerPadding = inner,
                        onBack = { navigator.back() },
                        sections = sections
                    )
                }

                composable(Routes.Info.USAGE) {
                    val sections = rememberInfoSections(
                        page = InfoPage.USAGE,
                        fallback = InfoContent.usageSections
                    )

                    InfoDetailScreen(
                        title = stringResource(R.string.usage_guide),
                        innerPadding = inner,
                        onBack = { navigator.back() },
                        sections = sections
                    )
                }

                composable(Routes.Settings.LANGUAGE) {
                    LanguageSelectionScreen(
                        innerPadding = inner,
                        onBack = { navigator.back() },
                        onLanguageChanged = onLanguageChanged
                    )
                }
            }
        }
    }
}

@Composable
private fun ExploreRoute(
    innerPadding: androidx.compose.foundation.layout.PaddingValues,
    navigator: ColorNavigator
) {
    ExploreScreen(
        innerPadding = innerPadding,
        onOpenLanguageSettings = { navigator.openLanguageSettings() },
        onOpenCopyright = { navigator.openInfoCopyright() },
        onOpenPrivacy = { navigator.openInfoPrivacy() },
        onOpenTerms = { navigator.openInfoTerms() },
        onOpenUsage = { navigator.openInfoUsage() }
    )
}

@Composable
private fun rememberInfoSections(
    page: InfoPage,
    fallback: List<InfoDetailSection>
): List<InfoDetailSection> {
    val context = LocalContext.current
    val locales = LocalConfiguration.current.locales
    val languageTag = if (locales.isEmpty) null else locales[0]?.toLanguageTag()
    val service = remember { InfoContentService(context) }

    val sections = produceState(initialValue = fallback, page, languageTag) {
        service.loadSections(page, languageTag, fallback) { updated ->
            value = updated
        }
    }
    return sections.value
}
