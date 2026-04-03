package com.primortex.color.service

import android.content.Context
import com.primortex.color.R
import com.primortex.color.app.Palette
import com.primortex.color.app.PickedColor
import com.primortex.color.i18n.AppStrings
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SeedService @Inject constructor(
    @ApplicationContext context: Context,
    private val colorService: ColorService,
    private val colorCatalogCoordinator: ColorCatalogCoordinator,
    private val paletteService: PaletteService,
    private val recentPicksService: RecentPicksService
) {
    companion object {
        private const val PREFS_NAME = "seed_flags"
        private const val PREF_KEY_SEEDED = "seeded_v2"
    }

    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    suspend fun seedOnInit() {
        if (prefs.getBoolean(PREF_KEY_SEEDED, false)) return

        colorCatalogCoordinator.loadNow()

        val now = System.currentTimeMillis()
        val welcomingStarterColors = listOf(
            0xFFA9D6E5.toInt(), // powder sky
            0xFFB8D8BA.toInt(), // calm sage
            0xFFFFB5A7.toInt(), // gentle coral
            0xFFFFD6A5.toInt(), // soft apricot
            0xFFFFF8F0.toInt()  // warm ivory
        ).map { argb ->
            PickedColor(
                argb = argb,
                name = colorService.localNameFromArgb(argb)
            )
        }

        val modernUiNeutrals = Palette(
            id = UUID.randomUUID().toString(),
            name = AppStrings.get(R.string.seed_modern_ui_neutrals_name),
            colors = welcomingStarterColors,
            tags = listOf(
                AppStrings.get(R.string.seed_modern_ui_neutrals_tag_ui),
                AppStrings.get(R.string.seed_modern_ui_neutrals_tag_neutral),
                AppStrings.get(R.string.seed_modern_ui_neutrals_tag_modern)
            ),
            note = AppStrings.get(R.string.seed_modern_ui_neutrals_note),
            createdAt = now,
            updatedAt = now
        )

        paletteService.seedPalettesIfEmpty(palettes = listOf(modernUiNeutrals))
        recentPicksService.seedHistoryIfEmpty(picks = welcomingStarterColors)
        recentPicksService.seedSavedIfEmpty(picks = welcomingStarterColors)

        prefs.edit().putBoolean(PREF_KEY_SEEDED, true).apply()
    }
}
