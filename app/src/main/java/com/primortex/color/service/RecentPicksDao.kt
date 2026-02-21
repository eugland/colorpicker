package com.primortex.color.service

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RecentPicksDao {
    @Query("SELECT * FROM recent_pick_history ORDER BY updatedAt DESC LIMIT :limit")
    fun observeHistory(limit: Int): Flow<List<RecentPickHistoryEntity>>

    @Query("SELECT * FROM saved_pick ORDER BY updatedAt DESC LIMIT :limit")
    fun observeSaved(limit: Int): Flow<List<SavedPickEntity>>

    @Query("SELECT * FROM recent_pick_history ORDER BY updatedAt DESC LIMIT :limit")
    suspend fun loadHistory(limit: Int): List<RecentPickHistoryEntity>

    @Query("SELECT * FROM saved_pick ORDER BY updatedAt DESC LIMIT :limit")
    suspend fun loadSaved(limit: Int): List<SavedPickEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(items: List<RecentPickHistoryEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSaved(items: List<SavedPickEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertHistory(item: RecentPickHistoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSaved(item: SavedPickEntity)

    @Query("DELETE FROM recent_pick_history")
    suspend fun clearHistory()

    @Query("DELETE FROM saved_pick")
    suspend fun clearSaved()

    @Query("DELETE FROM recent_pick_history WHERE argb = :argb")
    suspend fun deleteHistoryByArgb(argb: Int)

    @Query("DELETE FROM saved_pick WHERE argb = :argb")
    suspend fun deleteSavedByArgb(argb: Int)

    @Query("SELECT * FROM saved_pick WHERE argb = :argb LIMIT 1")
    suspend fun getSavedByArgb(argb: Int): SavedPickEntity?

    @Query(
        """
        DELETE FROM recent_pick_history
        WHERE argb NOT IN (
            SELECT argb FROM recent_pick_history ORDER BY updatedAt DESC LIMIT :limit
        )
        """
    )
    suspend fun trimHistory(limit: Int)

    @Query(
        """
        DELETE FROM saved_pick
        WHERE argb NOT IN (
            SELECT argb FROM saved_pick ORDER BY updatedAt DESC LIMIT :limit
        )
        """
    )
    suspend fun trimSaved(limit: Int)

    @Query("SELECT value FROM recent_picks_meta WHERE `key` = :key LIMIT 1")
    suspend fun getMeta(key: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putMeta(meta: RecentPicksMetaEntity)
}
