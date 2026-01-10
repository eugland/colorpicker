package com.primortex.color.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.primortex.color.R
import com.primortex.color.service.CrosshairShape
import com.primortex.color.service.CrosshairSize
import com.primortex.color.service.PickerSensitivity
import com.primortex.color.service.SettingsService
import com.primortex.color.service.ThemeMode
import com.primortex.color.ui.components.CrosshairIndicator
import com.primortex.color.ui.components.ScreenScaffold

@Composable
fun ExploreScreen(
    innerPadding: PaddingValues,
    onOpenLanguageSettings: () -> Unit,
    onOpenCopyright: () -> Unit,
    onOpenPrivacy: () -> Unit,
    onOpenTerms: () -> Unit,
    onOpenUsage: () -> Unit
) {
    val context = LocalContext.current
    val crosshairSize by SettingsService.crosshairSize.collectAsState()
    val crosshairShape by SettingsService.crosshairShape.collectAsState()
    val themeMode by SettingsService.themeMode.collectAsState()
    val pickerSensitivity by SettingsService.pickerSensitivity.collectAsState()
    val appLanguage by SettingsService.appLanguage.collectAsState()

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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.DarkMode,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                stringResource(R.string.theme),
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(start = 12.dp)
                            )
                        }
                        Text(
                            stringResource(R.string.theme_description),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ThemeMode.values().forEach { mode ->
                                FilterChip(
                                    selected = themeMode == mode,
                                    onClick = { SettingsService.setThemeMode(mode) },
                                    label = { Text(stringResource(mode.labelRes)) }
                                )
                            }
                        }
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
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CrosshairIndicator(
                            argb = MaterialTheme.colorScheme.primary.toArgb(),
                            size = crosshairSize,
                            shape = crosshairShape
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                stringResource(R.string.picker_crosshair),
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                stringResource(R.string.picker_crosshair_description),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    SectionHeader(stringResource(R.string.size))


                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CrosshairSize.values().forEach { size ->
                            FilterChip(
                                selected = crosshairSize == size,
                                onClick = { SettingsService.setCrosshairSize(size) },
                                label = { Text(stringResource(size.labelRes)) }
                            )
                        }
                    }


                    SectionHeader(stringResource(R.string.shape))


                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CrosshairShape.values().forEach { shape ->
                            FilterChip(
                                selected = crosshairShape == shape,
                                onClick = { SettingsService.setCrosshairShape(shape) },
                                label = { Text(stringResource(shape.labelRes)) }
                            )
                        }
                    }

                    SectionHeader(stringResource(R.string.sensitivity))

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            stringResource(R.string.sensitivity_description),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            PickerSensitivity.values().forEach { sensitivity ->
                                FilterChip(
                                    selected = pickerSensitivity == sensitivity,
                                    onClick = { SettingsService.setPickerSensitivity(sensitivity) },
                                    label = { Text(stringResource(sensitivity.labelRes)) }
                                )
                            }
                        }
                    }

                }
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
private fun SectionHeader(title: String) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            title.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        HorizontalDivider()
    }
}

@Composable
private fun SettingBlock(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            content = content
        )
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
