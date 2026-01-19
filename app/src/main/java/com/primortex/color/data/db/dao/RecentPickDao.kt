package com.primortex.color.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.primortex.color.data.db.entities.RecentPickEntity

@Dao
interface RecentPickDao {
    @Query("SELECT * FROM recent_picks WHERE kind = :kind ORDER BY createdAt DESC")
    suspend fun loadByKind(kind: String): List<RecentPickEntity>

    @Insert
    suspend fun insert(entity: RecentPickEntity)

    @Insert
    suspend fun insertAll(entities: List<RecentPickEntity>)

    @Query("DELETE FROM recent_picks WHERE kind = :kind")
    suspend fun clearByKind(kind: String)

    @Query("DELETE FROM recent_picks WHERE kind = :kind AND argb = :argb")
    suspend fun deleteByKindAndArgb(kind: String, argb: Int)

    @Query(
        "DELETE FROM recent_picks WHERE id IN (" +
            "SELECT id FROM recent_picks WHERE kind = :kind " +
            "ORDER BY createdAt DESC LIMIT -1 OFFSET :max" +
            ")"
    )
    suspend fun trimToLimit(kind: String, max: Int)
}
