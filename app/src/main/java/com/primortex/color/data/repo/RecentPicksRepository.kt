package com.primortex.color.data.repo

import com.primortex.color.app.PickedColor

interface RecentPicksRepository {
    suspend fun loadHistory(limit: Int): List<PickedColor>
    suspend fun loadSaved(limit: Int): List<PickedColor>
    suspend fun addHistory(pick: PickedColor, limit: Int)
    suspend fun addSaved(pick: PickedColor, limit: Int)
    suspend fun removeSaved(argb: Int)
    suspend fun replaceHistory(picks: List<PickedColor>)
    suspend fun replaceSaved(picks: List<PickedColor>)
    suspend fun clearHistory()
    suspend fun clearSaved()
}
