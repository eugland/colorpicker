package com.primortex.color.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
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
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.primortex.color.R
import com.primortex.color.service.SettingsService
import com.primortex.color.ui.components.CrosshairIndicator
import com.primortex.color.ui.components.ScreenScaffold

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
            verticalArrangement = Arrangement.spacedBy(20.dp)
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

            // --- App preferences card (Theme, Language, etc.) ---
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        stringResource(R.string.app_preferences),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                    )

                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ListItem(
                            headlineContent = { Text(stringResource(R.string.theme)) },
                            supportingContent = { Text(stringResource(themeMode.labelRes)) },
                            leadingContent = {
                                Icon(
                                    imageVector = Icons.Outlined.DarkMode,
                                    contentDescription = null
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onOpenThemeSettings()
                                }
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    ListItem(
                        headlineContent = { Text(stringResource(R.string.language)) },
                        supportingContent = { Text(stringResource(appLanguage.labelRes)) },
                        leadingContent = {
                            Icon(
                                imageVector = Icons.Outlined.Language,
                                contentDescription = null
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onOpenLanguageSettings()
                            }
                    )
                }
            }

            // --- Crosshair settings card ---
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.picker_crosshair)) },
                    supportingContent = {
                        Text(
                            stringResource(
                                R.string.picker_crosshair_summary,
                                stringResource(crosshairSize.labelRes),
                                stringResource(crosshairShape.labelRes),
                                stringResource(crosshairSensitivity.labelRes)
                            )
                        )
                    },
                    leadingContent = {
                        CrosshairIndicator(
                            argb = MaterialTheme.colorScheme.primary.toArgb(),
                            size = crosshairSize,
                            shape = crosshairShape
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onOpenCrosshairSettings()
                        }
                )
            }

            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        stringResource(R.string.information),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                    )

                    InfoLink(
                        title = stringResource(R.string.copyright_notice),
                        subtitle = stringResource(R.string.copyright_notice_description),
                        icon = Icons.Outlined.Gavel,
                        onClick = onOpenCopyright
                    )

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    InfoLink(
                        title = stringResource(R.string.privacy_statement),
                        subtitle = stringResource(R.string.privacy_statement_description),
                        icon = Icons.Outlined.Description,
                        onClick = onOpenPrivacy
                    )

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    InfoLink(
                        title = stringResource(R.string.terms_of_service),
                        subtitle = stringResource(R.string.terms_of_service_description),
                        icon = Icons.Outlined.Description,
                        onClick = onOpenTerms
                    )

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    InfoLink(
                        title = stringResource(R.string.usage_guide),
                        subtitle = stringResource(R.string.usage_guide_description),
                        icon = Icons.Outlined.Description,
                        onClick = onOpenUsage
                    )
                }
            }

            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        stringResource(R.string.feedback),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                    )

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

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    InfoLink(
                        title = stringResource(R.string.rate_us),
                        subtitle = stringResource(R.string.rate_us_description),
                        icon = Icons.Outlined.StarRate,
                        onClick = {
                            openUrl(
                                context,
                                "https://play.google.com/store/apps/details?id=com.primortex.color"
                            )
                        }
                    )

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    InfoLink(
                        title = stringResource(R.string.share_app),
                        subtitle = stringResource(R.string.share_app_description),
                        icon = Icons.Outlined.Share,
                        onClick = {
                            shareText(
                                context,
                                context.getString(
                                    R.string.share_app_text,
                                    "https://play.google.com/store/apps/details?id=com.primortex.color"
                                )
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun InfoLink(
    title: String,
    subtitle: String,
    icon: ImageVector,
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

private fun openUrl(context: Context, url: String) {
    openUrl(context, url, openInApp = false)
}

private fun openUrl(context: Context, url: String, openInApp: Boolean) {
    val uri = Uri.parse(url)
    if (openInApp) {
        val customTabsIntent = CustomTabsIntent.Builder().build()
        customTabsIntent.launchUrl(context, uri)
    } else {
        val intent = Intent(Intent.ACTION_VIEW, uri)
        context.startActivity(intent)
    }
}

private fun shareText(context: Context, text: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, null))
}
