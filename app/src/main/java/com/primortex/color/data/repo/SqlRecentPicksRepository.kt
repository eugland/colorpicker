package com.primortex.color.data.repo

import com.primortex.color.app.PickedColor
import com.primortex.color.data.db.ColorDatabaseApi
import com.primortex.color.data.db.entities.RecentPickEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SqlRecentPicksRepository(
    private val database: ColorDatabaseApi
) : RecentPicksRepository {

    override suspend fun loadHistory(limit: Int): List<PickedColor> = withContext(Dispatchers.IO) {
        database.recentPickDao()
            .loadByKind(KIND_HISTORY)
            .take(limit)
            .map { it.toModel() }
    }

    override suspend fun loadSaved(limit: Int): List<PickedColor> = withContext(Dispatchers.IO) {
        database.recentPickDao()
            .loadByKind(KIND_SAVED)
            .take(limit)
            .map { it.toModel() }
    }

    override suspend fun addHistory(pick: PickedColor, limit: Int) {
        upsertPick(pick, KIND_HISTORY, limit)
    }

    override suspend fun addSaved(pick: PickedColor, limit: Int) {
        upsertPick(pick, KIND_SAVED, limit)
    }

    override suspend fun removeSaved(argb: Int) = withContext(Dispatchers.IO) {
        database.recentPickDao().deleteByKindAndArgb(KIND_SAVED, argb)
    }

    override suspend fun replaceHistory(picks: List<PickedColor>) = withContext(Dispatchers.IO) {
        database.runInTransaction {
            val dao = database.recentPickDao()
            dao.clearByKind(KIND_HISTORY)
            dao.insertAll(picks.map { it.toEntity(KIND_HISTORY) })
            dao.trimToLimit(KIND_HISTORY, picks.size)
        }
    }

    override suspend fun replaceSaved(picks: List<PickedColor>) = withContext(Dispatchers.IO) {
        database.runInTransaction {
            val dao = database.recentPickDao()
            dao.clearByKind(KIND_SAVED)
            dao.insertAll(picks.map { it.toEntity(KIND_SAVED) })
            dao.trimToLimit(KIND_SAVED, picks.size)
        }
    }

    override suspend fun clearHistory() = withContext(Dispatchers.IO) {
        database.recentPickDao().clearByKind(KIND_HISTORY)
    }

    override suspend fun clearSaved() = withContext(Dispatchers.IO) {
        database.recentPickDao().clearByKind(KIND_SAVED)
    }

    private suspend fun upsertPick(pick: PickedColor, kind: String, limit: Int) =
        withContext(Dispatchers.IO) {
            database.runInTransaction {
                val dao = database.recentPickDao()
                dao.deleteByKindAndArgb(kind, pick.argb)
                dao.insert(pick.toEntity(kind))
                dao.trimToLimit(kind, limit)
            }
        }

    private fun PickedColor.toEntity(kind: String): RecentPickEntity {
        return RecentPickEntity(
            argb = argb,
            name = name,
            kind = kind,
            createdAt = System.currentTimeMillis()
        )
    }

    private fun RecentPickEntity.toModel(): PickedColor {
        return PickedColor(argb = argb, name = name)
    }

    private companion object {
        const val KIND_HISTORY = "history"
        const val KIND_SAVED = "saved"
    }
}
