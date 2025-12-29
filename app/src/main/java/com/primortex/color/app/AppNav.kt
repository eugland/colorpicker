package com.primortex.color.app

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.primortex.color.screens.CameraHomeScreen
import com.primortex.color.screens.LiveCameraScreen

@Composable
fun AppNav() {
    val nav = rememberNavController()

    NavHost(navController = nav, startDestination = Routes.HOME) {

        composable(Routes.HOME) {
            CameraHomeScreen(
                onOpenCamera = { nav.navigate(Routes.LIVE_CAMERA) },
                onOpenAlbum = { nav.navigate(Routes.LIVE_CAMERA) } // album is inside camera screen button
            )
        }

        composable(Routes.LIVE_CAMERA) {
            LiveCameraScreen(
                onBack = { nav.popBackStack() },
                onOpenPhotoPick = { uriString ->
                    nav.navigate("${Routes.PHOTO_PICK}?uri=${java.net.URLEncoder.encode(uriString, "UTF-8")}")
                }
            )
        }
//
//        composable(
//            route = "${Routes.PHOTO_PICK}?uri={uri}",
//            arguments = listOf(navArgument("uri") { type = NavType.StringType; defaultValue = "" })
//        ) { backStack ->
//            val uri = java.net.URLDecoder.decode(backStack.arguments?.getString("uri") ?: "", "UTF-8")
//            PhotoPickScreen(photoUri = uri, onBack = { nav.popBackStack() })
//        }
    }
}
