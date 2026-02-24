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
        private const val PREF_KEY_SEEDED = "seeded_v1"
    }

    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    suspend fun seedOnInit() {
        if (prefs.getBoolean(PREF_KEY_SEEDED, false)) return

        colorCatalogCoordinator.loadNow()

        val now = System.currentTimeMillis()
        val modernUiNeutralColors = listOf(
            0xFF0F172A.toInt(),
            0xFF475569.toInt(),
            0xFFA1A1AA.toInt(),
            0xFF0EA5E9.toInt(),
            0xFF10B981.toInt()
        ).map { argb ->
            PickedColor(
                argb = argb,
                name = colorService.localNameFromArgb(argb)
            )
        }

        val modernUiNeutrals = Palette(
            id = UUID.randomUUID().toString(),
            name = AppStrings.get(R.string.seed_modern_ui_neutrals_name),
            colors = modernUiNeutralColors,
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
        recentPicksService.seedHistoryIfEmpty(picks = modernUiNeutralColors)
        recentPicksService.seedSavedIfEmpty(picks = modernUiNeutralColors)

        prefs.edit().putBoolean(PREF_KEY_SEEDED, true).apply()
    }
}
