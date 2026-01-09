package com.primortex.color.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.primortex.color.data.entity.RecentHistoryEntity
import com.primortex.color.data.entity.SavedColorEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecentPickDao {
    @Query("SELECT * FROM recent_history ORDER BY createdAt DESC LIMIT :limit")
    fun observeHistory(limit: Int): Flow<List<RecentHistoryEntity>>

    @Query("SELECT * FROM saved_colors ORDER BY createdAt DESC LIMIT :limit")
    fun observeSaved(limit: Int): Flow<List<SavedColorEntity>>

    @Query("SELECT COUNT(*) FROM recent_history")
    suspend fun historyCount(): Int

    @Query("SELECT COUNT(*) FROM saved_colors")
    suspend fun savedCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertHistory(entity: RecentHistoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertHistory(entities: List<RecentHistoryEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSaved(entity: SavedColorEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSaved(entities: List<SavedColorEntity>)

    @Query(
        "DELETE FROM recent_history WHERE argb NOT IN (" +
            "SELECT argb FROM recent_history ORDER BY createdAt DESC LIMIT :limit"
    )
    suspend fun trimHistory(limit: Int)

    @Query(
        "DELETE FROM saved_colors WHERE argb NOT IN (" +
            "SELECT argb FROM saved_colors ORDER BY createdAt DESC LIMIT :limit"
    )
    suspend fun trimSaved(limit: Int)

    @Query("DELETE FROM saved_colors WHERE argb = :argb")
    suspend fun deleteSaved(argb: Int)

    @Query("DELETE FROM recent_history")
    suspend fun clearHistory()

    @Query("DELETE FROM saved_colors")
    suspend fun clearSaved()
}
