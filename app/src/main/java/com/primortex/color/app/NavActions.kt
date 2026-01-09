package com.primortex.color.app

import androidx.navigation.NavController

data class NavigateOptions(
    val popUpToRoute: String? = null,
    val inclusive: Boolean = false,
    val launchSingleTop: Boolean = false
)

interface NavActions {
    fun navigate(route: String, options: NavigateOptions = NavigateOptions())
    fun popBackStack(): Boolean
}

class NavControllerActions(
    private val nav: NavController
) : NavActions {
    override fun navigate(route: String, options: NavigateOptions) {
        nav.navigate(route) {
            options.popUpToRoute?.let { routeToPop ->
                popUpTo(routeToPop) { inclusive = options.inclusive }
            }
            launchSingleTop = options.launchSingleTop
        }
    }

    override fun popBackStack(): Boolean = nav.popBackStack()
}
