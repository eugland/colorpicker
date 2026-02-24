package com.primortex.color.features.explore

import android.content.Context
import android.content.Intent
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.primortex.color.R

@Composable
fun ExploreRoute(
    innerPadding: PaddingValues,
    onNavigate: (ExploreDestination) -> Unit
) {
    val viewModel: ExploreViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    ExploreScreen(
        uiState = uiState,
        innerPadding = innerPadding,
        onAction = { action ->
            when (action) {
                ExploreUiAction.OpenThemeSettings -> onNavigate(ExploreDestination.ThemeSettings)
                ExploreUiAction.OpenLanguageSettings -> onNavigate(ExploreDestination.LanguageSettings)
                ExploreUiAction.OpenCrosshairSettings -> onNavigate(ExploreDestination.CrosshairSettings)
                ExploreUiAction.OpenCopyright -> onNavigate(ExploreDestination.Copyright)
                ExploreUiAction.OpenPrivacy -> onNavigate(ExploreDestination.Privacy)
                ExploreUiAction.OpenTerms -> onNavigate(ExploreDestination.Terms)
                ExploreUiAction.OpenUsage -> onNavigate(ExploreDestination.Usage)
                ExploreUiAction.OpenFeedbackForm -> openUrl(
                    context,
                    "https://docs.google.com/forms/d/e/1FAIpQLScd5C3ut3O1nHnIBKtq9QD7FkNuNAKIjzfZyRRtZKRHUptkrQ/viewform?usp=dialog",
                    openInApp = true
                )

                ExploreUiAction.OpenRateUs -> openUrl(
                    context,
                    "https://play.google.com/store/apps/details?id=com.primortex.color",
                    openInApp = true
                )

                ExploreUiAction.ShareApp -> shareText(context)
            }
        }
    )
}

private fun openUrl(context: Context, url: String, openInApp: Boolean) {
    val uri = url.toUri()
    if (openInApp) {
        val customTabsIntent = CustomTabsIntent.Builder().build()
        customTabsIntent.launchUrl(context, uri)
    } else {
        val intent = Intent(Intent.ACTION_VIEW, uri)
        context.startActivity(intent)
    }
}

private fun shareText(context: Context) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        val text = context.getString(
            R.string.share_app_text,
            "https://play.google.com/store/apps/details?id=com.primortex.color"
        )
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, null))
}
