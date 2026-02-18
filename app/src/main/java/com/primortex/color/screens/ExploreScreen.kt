package com.primortex.color.screens

import android.content.Context
import android.content.Intent
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowForwardIos
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Feedback
import androidx.compose.material.icons.outlined.Gavel
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.StarRate
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import com.primortex.color.i18n.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.primortex.color.R
import com.primortex.color.service.SettingsService
import com.primortex.color.ui.components.CrosshairIndicator
import com.primortex.color.ui.components.ScreenScaffold

private val CardOuterSpacing = 20.dp
private val CardHeaderPaddingH = 16.dp
private val CardHeaderPaddingV = 12.dp
private val DividerInsetH = 16.dp

@Composable
fun ExploreScreen(
    innerPadding: PaddingValues,
    onOpenLanguageSettings: () -> Unit,
    onOpenThemeSettings: () -> Unit,
    onOpenCrosshairSettings: () -> Unit,
    onOpenCopyright: () -> Unit,
    onOpenPrivacy: () -> Unit,
    onOpenTerms: () -> Unit,
    onOpenUsage: () -> Unit
) {
    val context = LocalContext.current
    val crosshairSize by SettingsService.crosshairSize.collectAsState()
    val crosshairShape by SettingsService.crosshairShape.collectAsState()
    val themeMode by SettingsService.themeMode.collectAsState()
    val appLanguage by SettingsService.appLanguage.collectAsState()
    val crosshairSensitivity by SettingsService.pickerSensitivity.collectAsState()

    ScreenScaffold(R.string.explore, innerPadding) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(CardOuterSpacing)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    stringResource(R.string.settings_title),
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    stringResource(R.string.settings_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // App preferences
            StandardCard(title = stringResource(R.string.app_preferences)) {
                RowLink(
                    title = stringResource(R.string.theme),
                    subtitle = stringResource(themeMode.labelRes),
                    leadingIcon = Icons.Outlined.DarkMode,
                    onClick = onOpenThemeSettings
                )
                RowDivider()
                RowLink(
                    title = stringResource(R.string.language),
                    subtitle = stringResource(appLanguage.labelRes),
                    leadingIcon = Icons.Outlined.Language,
                    onClick = onOpenLanguageSettings
                )
            }

            // Crosshair
            StandardCard(title = stringResource(R.string.live_picking)) {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.picker_crosshair)) },
                    supportingContent = {
                        Text(
                            stringResource(
                                R.string.picker_crosshair_summary,
                                stringResource(crosshairSize.labelRes),
                                stringResource(crosshairShape.labelRes),
                                stringResource(crosshairSensitivity.labelRes)
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    leadingContent = {
                        CrosshairIndicator(
                            argb = MaterialTheme.colorScheme.primary.toArgb(),
                            size = crosshairSize,
                            shape = crosshairShape,
                            displaySize = 22.dp
                        )
                    },
                    trailingContent = {
                        Icon(
                            imageVector = Icons.Outlined.ArrowForwardIos,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onOpenCrosshairSettings)
                )
            }

            // Information
            StandardCard(title = stringResource(R.string.information)) {
                InfoLink(
                    title = stringResource(R.string.copyright_notice),
                    subtitle = stringResource(R.string.copyright_notice_description),
                    icon = Icons.Outlined.Gavel,
                    onClick = onOpenCopyright
                )
                RowDivider()
                InfoLink(
                    title = stringResource(R.string.privacy_statement),
                    subtitle = stringResource(R.string.privacy_statement_description),
                    icon = Icons.Outlined.Description,
                    onClick = onOpenPrivacy
                )
                RowDivider()
                InfoLink(
                    title = stringResource(R.string.terms_of_service),
                    subtitle = stringResource(R.string.terms_of_service_description),
                    icon = Icons.Outlined.Description,
                    onClick = onOpenTerms
                )
                RowDivider()
                InfoLink(
                    title = stringResource(R.string.usage_guide),
                    subtitle = stringResource(R.string.usage_guide_description),
                    icon = Icons.Outlined.Description,
                    onClick = onOpenUsage
                )
            }

            // Feedback
            StandardCard(title = stringResource(R.string.feedback)) {
                InfoLink(
                    title = stringResource(R.string.send_feedback),
                    subtitle = stringResource(R.string.send_feedback_description),
                    icon = Icons.Outlined.Feedback,
                    onClick = {
                        openUrl(
                            context,
                            "https://docs.google.com/forms/d/e/1FAIpQLScd5C3ut3O1nHnIBKtq9QD7FkNuNAKIjzfZyRRtZKRHUptkrQ/viewform?usp=dialog",
                            openInApp = true
                        )
                    }
                )
                RowDivider()
                InfoLink(
                    title = stringResource(R.string.rate_us),
                    subtitle = stringResource(R.string.rate_us_description),
                    icon = Icons.Outlined.StarRate,
                    onClick = {
                        openUrl(
                            context,
                            "https://play.google.com/store/apps/details?id=com.primortex.color",
                            openInApp = true
                        )
                    }
                )
                RowDivider()
                InfoLink(
                    title = stringResource(R.string.share_app),
                    subtitle = stringResource(R.string.share_app_description),
                    icon = Icons.Outlined.Share,
                    onClick = {
                        shareText(
                            context
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun StandardCard(
    title: String? = null,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        tonalElevation = 1.dp
    ) {
        Column {
            if (title != null) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceColorAtElevation(4.dp))
                        .padding(
                            horizontal = CardHeaderPaddingH,
                            vertical = CardHeaderPaddingV
                        )
                )
            }
            content()
        }
    }
}

@Composable
private fun RowDivider() {
    HorizontalDivider(modifier = Modifier.padding(horizontal = DividerInsetH))
}

@Composable
private fun RowLink(
    title: String,
    subtitle: String,
    leadingIcon: ImageVector,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = {
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        leadingContent = {
            Icon(imageVector = leadingIcon, contentDescription = null)
        },
        trailingContent = {
            Icon(
                imageVector = Icons.Outlined.ArrowForwardIos,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    )
}

@Composable
private fun InfoLink(
    title: String,
    subtitle: String? = null,
    icon: ImageVector,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = { Text(title) },
        leadingContent = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        trailingContent = {
            Icon(
                imageVector = Icons.Outlined.ArrowForwardIos,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
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

