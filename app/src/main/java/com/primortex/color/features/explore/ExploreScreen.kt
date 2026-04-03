package com.primortex.color.features.explore

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
import androidx.compose.ui.Modifier
import androidx.compose.material3.LocalContentColor
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.primortex.color.R
import com.primortex.color.i18n.stringResource
import com.primortex.color.ui.components.CrosshairIndicator
import com.primortex.color.ui.components.ScreenScaffold

private val CardOuterSpacing = 20.dp
private val CardHeaderPaddingH = 16.dp
private val CardHeaderPaddingV = 12.dp
private val DividerInsetH = 16.dp

@Composable
fun ExploreScreen(
    uiState: ExploreUiState,
    innerPadding: PaddingValues,
    onAction: (ExploreUiAction) -> Unit
) {
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

            StandardCard(title = stringResource(R.string.app_preferences)) {
                RowLink(
                    title = stringResource(R.string.theme),
                    subtitle = stringResource(uiState.themeMode.labelRes),
                    leadingIcon = Icons.Outlined.DarkMode,
                    onClick = { onAction(ExploreUiAction.OpenThemeSettings) }
                )
                RowDivider()
                RowLink(
                    title = stringResource(R.string.language),
                    subtitle = stringResource(uiState.appLanguage.labelRes),
                    leadingIcon = Icons.Outlined.Language,
                    onClick = { onAction(ExploreUiAction.OpenLanguageSettings) }
                )
            }

            StandardCard(title = stringResource(R.string.live_picking)) {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.picker_crosshair)) },
                    supportingContent = {
                        Text(
                            stringResource(
                                R.string.picker_crosshair_summary,
                                stringResource(uiState.crosshairSize.labelRes),
                                stringResource(uiState.crosshairShape.labelRes),
                                stringResource(uiState.crosshairSensitivity.labelRes)
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    leadingContent = {
                        CrosshairIndicator(
                            argb = MaterialTheme.colorScheme.primary.toArgb(),
                            size = uiState.crosshairSize,
                            shape = uiState.crosshairShape,
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
                        .clickable { onAction(ExploreUiAction.OpenCrosshairSettings) }
                )
            }

            StandardCard(title = stringResource(R.string.information)) {
                InfoLink(
                    title = stringResource(R.string.copyright_notice),
                    subtitle = stringResource(R.string.copyright_notice_description),
                    icon = Icons.Outlined.Gavel,
                    onClick = { onAction(ExploreUiAction.OpenCopyright) }
                )
                RowDivider()
                InfoLink(
                    title = stringResource(R.string.privacy_statement),
                    subtitle = stringResource(R.string.privacy_statement_description),
                    icon = Icons.Outlined.Description,
                    onClick = { onAction(ExploreUiAction.OpenPrivacy) }
                )
                RowDivider()
                InfoLink(
                    title = stringResource(R.string.terms_of_service),
                    subtitle = stringResource(R.string.terms_of_service_description),
                    icon = Icons.Outlined.Description,
                    onClick = { onAction(ExploreUiAction.OpenTerms) }
                )
                RowDivider()
                InfoLink(
                    title = stringResource(R.string.usage_guide),
                    subtitle = stringResource(R.string.usage_guide_description),
                    icon = Icons.Outlined.Description,
                    onClick = { onAction(ExploreUiAction.OpenUsage) }
                )
            }

            StandardCard(title = stringResource(R.string.feedback)) {
                InfoLink(
                    title = stringResource(R.string.send_feedback),
                    subtitle = stringResource(R.string.send_feedback_description),
                    icon = Icons.Outlined.Feedback,
                    onClick = { onAction(ExploreUiAction.OpenFeedbackForm) }
                )
                RowDivider()
                InfoLink(
                    title = stringResource(R.string.rate_us),
                    subtitle = stringResource(R.string.rate_us_description),
                    icon = Icons.Outlined.StarRate,
                    onClick = { onAction(ExploreUiAction.OpenRateUs) }
                )
                RowDivider()
                InfoLink(
                    title = stringResource(R.string.share_app),
                    subtitle = stringResource(R.string.share_app_description),
                    icon = Icons.Outlined.Share,
                    onClick = { onAction(ExploreUiAction.ShareApp) }
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
                        .padding(horizontal = CardHeaderPaddingH, vertical = CardHeaderPaddingV)
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
    subtitle: String? = null,
    leadingIcon: ImageVector,
    iconTint: Color = Color.Unspecified,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = if (subtitle != null) {
            {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else null,
        leadingContent = {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                tint = if (iconTint == Color.Unspecified) LocalContentColor.current else iconTint
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

@Composable
private fun InfoLink(
    title: String,
    subtitle: String? = null,
    icon: ImageVector,
    onClick: () -> Unit
) {
    RowLink(
        title = title,
        subtitle = subtitle,
        leadingIcon = icon,
        iconTint = MaterialTheme.colorScheme.primary,
        onClick = onClick
    )
}
