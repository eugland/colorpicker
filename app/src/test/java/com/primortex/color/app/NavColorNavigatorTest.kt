package com.primortex.color.app

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NavColorNavigatorTest {
    @Test
    fun `open palette tab navigates with pop up options`() {
        val actions = RecordingNavActions()
        val navigator = NavColorNavigator(actions)

        navigator.openPaletteTab()

        val (route, options) = actions.navigations.last()
        assertEquals(Routes.Tab.PALETTE, route)
        assertEquals(
            NavigateOptions(
                popUpToRoute = Routes.Tab.CAMERA,
                inclusive = false,
                launchSingleTop = true
            ),
            options
        )
    }

    @Test
    fun `open camera tab resets stack`() {
        val actions = RecordingNavActions()
        val navigator = NavColorNavigator(actions)

        navigator.openCameraTab()

        val (route, options) = actions.navigations.last()
        assertEquals(Routes.Tab.CAMERA, route)
        assertEquals(
            NavigateOptions(
                popUpToRoute = Routes.Tab.CAMERA,
                inclusive = true,
                launchSingleTop = true
            ),
            options
        )
    }

    @Test
    fun `open explore tab navigates without clearing camera`() {
        val actions = RecordingNavActions()
        val navigator = NavColorNavigator(actions)

        navigator.openExploreTab()

        val (route, options) = actions.navigations.last()
        assertEquals(Routes.Tab.EXPLORE, route)
        assertEquals(
            NavigateOptions(
                popUpToRoute = Routes.Tab.CAMERA,
                inclusive = false,
                launchSingleTop = true
            ),
            options
        )
    }

    @Test
    fun `open live camera uses direct route`() {
        val actions = RecordingNavActions()
        val navigator = NavColorNavigator(actions)

        navigator.openLiveCamera()

        val (route, options) = actions.navigations.last()
        assertEquals(Routes.Camera.LIVE, route)
        assertEquals(NavigateOptions(), options)
    }

    @Test
    fun `back pops stack`() {
        val actions = RecordingNavActions()
        val navigator = NavColorNavigator(actions)

        navigator.back()

        assertTrue(actions.popBackCalls > 0)
    }
}

private class RecordingNavActions : NavActions {
    val navigations = mutableListOf<Pair<String, NavigateOptions>>()
    var popBackCalls = 0

    override fun navigate(route: String, options: NavigateOptions) {
        navigations.add(route to options)
    }

    override fun popBackStack(): Boolean {
        popBackCalls += 1
        return true
    }
}
