package com.primortex.color.ui

import androidx.annotation.StringRes
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.staticCompositionLocalOf
import com.primortex.color.i18n.AppStrings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class SnackbarController(private val scope: CoroutineScope) {
    val hostState = SnackbarHostState()

    fun showMessage(message: String) {
        scope.launch {
            hostState.currentSnackbarData?.dismiss()
            hostState.showSnackbar(message)
        }
    }

    fun showMessage(@StringRes messageRes: Int, vararg formatArgs: Any) {
        showMessage(AppStrings.get(messageRes, *formatArgs))
    }
}

val LocalSnackbarController = staticCompositionLocalOf<SnackbarController> {
    error("SnackbarController not provided")
}

@Composable
fun rememberSnackbarController(): SnackbarController {
    val scope = rememberCoroutineScope()
    return remember(scope) { SnackbarController(scope) }
}

