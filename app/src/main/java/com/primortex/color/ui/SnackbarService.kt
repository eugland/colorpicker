package com.primortex.color.ui

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class SnackbarService(private val scope: CoroutineScope) {
    val hostState = SnackbarHostState()

    fun showMessage(message: String) {
        scope.launch {
            hostState.currentSnackbarData?.dismiss()
            hostState.showSnackbar(message)
        }
    }
}

val LocalSnackbarService = staticCompositionLocalOf<SnackbarService> {
    error("SnackbarService not provided")
}

@Composable
fun rememberSnackbarService(): SnackbarService {
    val scope = rememberCoroutineScope()
    return remember(scope) { SnackbarService(scope) }
}
