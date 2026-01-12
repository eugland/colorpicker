package com.primortex.color.screens

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.primortex.color.R
import com.primortex.color.service.argbToHex
import com.primortex.color.service.rgbDistSq
import com.primortex.color.ui.components.ScreenScaffold

private enum class MatchReason(@StringRes val labelRes: Int) {
    ExactName(R.string.ip_lookup_match_name),
    ExactColor(R.string.ip_lookup_match_exact),
    SimilarColor(R.string.ip_lookup_match_similar)
}

private data class IpColorEntry(
    val name: String,
    val argb: Int,
    val owner: String,
    @StringRes val statusRes: Int
)

private data class ColorOption(
    val name: String,
    val argb: Int
)

private data class IpMatch(
    val entry: IpColorEntry,
    val reason: MatchReason,
    val distance: Int
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun IpLookupScreen(
    innerPadding: PaddingValues,
    onBack: () -> Unit
) {
    var query by remember { mutableStateOf("") }

    val ipColors = remember {
        listOf(
            IpColorEntry(
                name = "Tiffany Blue",
                argb = 0xFF81D8D0.toInt(),
                owner = "Tiffany & Co.",
                statusRes = R.string.ip_lookup_status_trademark
            ),
            IpColorEntry(
                name = "Barbie Pink",
                argb = 0xFFFF4DA6.toInt(),
                owner = "Mattel",
                statusRes = R.string.ip_lookup_status_trademark
            ),
            IpColorEntry(
                name = "UPS Brown",
                argb = 0xFF351C15.toInt(),
                owner = "UPS",
                statusRes = R.string.ip_lookup_status_trademark
            ),
            IpColorEntry(
                name = "Cadbury Purple",
                argb = 0xFF3A2A6A.toInt(),
                owner = "Cadbury",
                statusRes = R.string.ip_lookup_status_trademark
            ),
            IpColorEntry(
                name = "Coca-Cola Red",
                argb = 0xFFE61A23.toInt(),
                owner = "The Coca-Cola Company",
                statusRes = R.string.ip_lookup_status_trademark
            )
        )
    }

    val ipFreePalette = remember {
        listOf(
            ColorOption("Ocean Teal", 0xFF2CA6A4.toInt()),
            ColorOption("Desert Sand", 0xFFD8B98A.toInt()),
            ColorOption("Citrus Lime", 0xFF9ACD32.toInt()),
            ColorOption("Royal Indigo", 0xFF4B3F72.toInt()),
            ColorOption("Slate Blue", 0xFF5A6B9C.toInt()),
            ColorOption("Sunset Coral", 0xFFEF6A5B.toInt()),
            ColorOption("Fog Gray", 0xFF9AA4B2.toInt()),
            ColorOption("Midnight Navy", 0xFF1B2A41.toInt())
        )
    }

    val trimmedQuery = query.trim()
    val queryArgb = remember(trimmedQuery) { parseArgb(trimmedQuery) }
    val matches = remember(trimmedQuery, queryArgb) {
        resolveMatches(trimmedQuery, queryArgb, ipColors)
    }
    val alternatives = remember(queryArgb) {
        queryArgb?.let { argb -> ipFreePalette.sortedBy { rgbDistSq(it.argb, argb) }.take(4) }
            ?: emptyList()
    }

    ScreenScaffold(
        titleRes = R.string.ip_lookup,
        innerPadding = innerPadding,
        onBack = onBack
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(
                stringResource(R.string.ip_lookup_disclaimer),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text(stringResource(R.string.ip_lookup_hint)) },
                supportingText = { Text(stringResource(R.string.ip_lookup_supporting)) },
                modifier = Modifier.fillMaxWidth()
            )

            if (trimmedQuery.isEmpty()) {
                Text(
                    stringResource(R.string.ip_lookup_featured_title),
                    style = MaterialTheme.typography.titleMedium
                )
                FeaturedIpColorCard(entry = ipColors.first())

                Text(
                    stringResource(R.string.ip_lookup_catalog_title),
                    style = MaterialTheme.typography.titleMedium
                )
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ipColors.forEach { entry ->
                        IpColorSwatch(entry = entry)
                    }
                }
            } else {
                Text(
                    stringResource(R.string.ip_lookup_matches_title),
                    style = MaterialTheme.typography.titleMedium
                )

                if (matches.isEmpty()) {
                    Text(
                        stringResource(R.string.ip_lookup_matches_empty),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        matches.forEach { match ->
                            IpMatchCard(match = match)
                        }
                    }
                }

                Text(
                    stringResource(R.string.ip_lookup_alternatives_title),
                    style = MaterialTheme.typography.titleMedium
                )

                if (alternatives.isEmpty()) {
                    Text(
                        stringResource(R.string.ip_lookup_alternatives_empty),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        alternatives.forEach { option ->
                            AlternativeSwatch(option = option)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FeaturedIpColorCard(entry: IpColorEntry) {
    Card(shape = RoundedCornerShape(20.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(Color(entry.argb))
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(entry.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    stringResource(R.string.ip_lookup_owner, entry.owner),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    stringResource(entry.statusRes),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium
                )
            }
            Text(
                argbToHex(entry.argb),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun IpColorSwatch(entry: IpColorEntry) {
    Card(shape = RoundedCornerShape(16.dp)) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .widthIn(min = 140.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(entry.argb))
            )
            Text(entry.name, style = MaterialTheme.typography.titleSmall)
            Text(
                stringResource(entry.statusRes),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun IpMatchCard(match: IpMatch) {
    Card(shape = RoundedCornerShape(16.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(match.entry.argb))
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(match.entry.name, style = MaterialTheme.typography.titleSmall)
                Text(
                    stringResource(R.string.ip_lookup_owner, match.entry.owner),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    stringResource(match.reason.labelRes),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Text(
                argbToHex(match.entry.argb),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AlternativeSwatch(option: ColorOption) {
    Card(shape = RoundedCornerShape(16.dp)) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .widthIn(min = 120.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(option.argb))
            )
            Text(option.name, style = MaterialTheme.typography.titleSmall)
            Text(
                argbToHex(option.argb),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun parseArgb(input: String): Int? {
    if (input.isBlank()) return null
    val trimmed = input.trim()
    val hex = trimmed.removePrefix("#")
    if (hex.length !in setOf(3, 6)) return null
    val normalized = if (hex.length == 3) {
        hex.map { "$it$it" }.joinToString("")
    } else {
        hex
    }
    if (!normalized.matches(Regex("^[0-9A-Fa-f]{6}$"))) return null
    return (0xFF shl 24) or normalized.toInt(16)
}

private fun resolveMatches(
    query: String,
    queryArgb: Int?,
    ipColors: List<IpColorEntry>
): List<IpMatch> {
    val matches = mutableListOf<IpMatch>()
    val lowered = query.lowercase()
    val exactByName = ipColors.filter { it.name.lowercase() == lowered }
    exactByName.forEach { entry ->
        matches.add(IpMatch(entry, MatchReason.ExactName, 0))
    }

    if (queryArgb != null) {
        val threshold = 22
        val thresholdSq = threshold * threshold
        ipColors.forEach { entry ->
            val dist = rgbDistSq(queryArgb, entry.argb)
            when {
                dist == 0 -> matches.add(IpMatch(entry, MatchReason.ExactColor, dist))
                dist <= thresholdSq -> matches.add(IpMatch(entry, MatchReason.SimilarColor, dist))
            }
        }
    }

    return matches
        .distinctBy { it.entry.name }
        .sortedWith(compareBy<IpMatch> { it.reason.ordinal }.thenBy { it.distance })
}
