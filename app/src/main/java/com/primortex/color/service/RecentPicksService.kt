package com.primortex.color.service

import android.content.Context
import android.util.Log
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.primortex.color.analytics.AnalyticsTracker
import com.primortex.color.app.PickedColor
import com.primortex.color.service.recentpicks.RecentPickHistoryEntity
import com.primortex.color.service.recentpicks.RecentPicksDatabase
import com.primortex.color.service.recentpicks.SavedPickEntity
import com.primortex.color.service.recentpicks.toPickedColor
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

@Singleton
class RecentPicksService @Inject constructor(
    @ApplicationContext context: Context,
    private val analyticsTracker: AnalyticsTracker
) {
    private companion object {
        const val MAX = 100
        const val PREFS_NAME = "recent_picks_flags"
        const val PREF_KEY_FIRST_PICK_LOGGED = "first_pick_logged_v1"
        const val PREF_KEY_FIRST_SAVE_LOGGED = "first_saved_logged_v1"
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP TABLE IF EXISTS recent_picks_meta")
            }
        }
    }

    private object RecentPicksDbHolder {
        val executor = Executors.newSingleThreadExecutor()
        @Volatile private var instance: RecentPicksDatabase? = null

        fun get(context: Context): RecentPicksDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    RecentPicksDatabase::class.java,
                    "recent_picks.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .setQueryExecutor(executor)
                    .setTransactionExecutor(executor)
                    .build()
                    .also { instance = it }
            }
        }
    }

    private val appContext = context.applicationContext
    private val dbExecutor = RecentPicksDbHolder.executor
    private val scope = CoroutineScope(SupervisorJob() + dbExecutor.asCoroutineDispatcher())
    private val db: RecentPicksDatabase = RecentPicksDbHolder.get(appContext)
    private val dao = db.recentPicksDao()
    private val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val history: StateFlow<List<PickedColor>> = dao.observeHistory(MAX)
        .map { rows -> rows.map { it.toPickedColor() } }
        .stateIn(scope, SharingStarted.Eagerly, emptyList())
    val saved: StateFlow<List<PickedColor>> = dao.observeSaved(MAX)
        .map { rows -> rows.map { it.toPickedColor() } }
        .stateIn(scope, SharingStarted.Eagerly, emptyList())
    private var firstPickLogged = false
    private var firstSavedLogged = false

    init {
        firstPickLogged = prefs.getBoolean(PREF_KEY_FIRST_PICK_LOGGED, false)
        firstSavedLogged = prefs.getBoolean(PREF_KEY_FIRST_SAVE_LOGGED, false)
        Log.d("RecentPicksService", "Recent picks Room flow initialized")
    }

    fun addPick(pick: PickedColor, source: String = "unknown") {
        analyticsTracker.logColorPicked(pick, source)
        if (!firstPickLogged) {
            analyticsTracker.logFirstColorPick(pick)
            firstPickLogged = true
            prefs.edit().putBoolean(PREF_KEY_FIRST_PICK_LOGGED, true).apply()
        }
        scope.launch {
            dao.upsertHistory(
                RecentPickHistoryEntity(
                    argb = pick.argb,
                    name = pick.name,
                    updatedAt = System.currentTimeMillis()
                )
            )
            dao.trimHistory(MAX)
        }
    }

    fun clear() {
        analyticsTracker.logRecentsCleared()
        scope.launch {
            dao.clearHistory()
        }
    }

    fun clearSaved() {
        analyticsTracker.logSavedCleared()
        scope.launch {
            dao.clearSaved()
        }
    }

    fun addSaved(pick: PickedColor) {
        scope.launch {
            dao.upsertSaved(
                SavedPickEntity(
                    argb = pick.argb,
                    name = pick.name,
                    updatedAt = System.currentTimeMillis()
                )
            )
            dao.trimSaved(MAX)
            analyticsTracker.logColorSaved(pick, action = "saved")
            if (!firstSavedLogged) {
                analyticsTracker.logFirstColorSaved(pick)
                firstSavedLogged = true
                prefs.edit().putBoolean(PREF_KEY_FIRST_SAVE_LOGGED, true).apply()
            }
        }
    }

    fun removeSaved(argb: Int) {
        scope.launch {
            val removed = dao.getSavedByArgb(argb)?.toPickedColor()
            dao.deleteSavedByArgb(argb)
            removed?.let { analyticsTracker.logColorSaved(it, action = "removed") }
        }
    }

    fun toggleSaved(pick: PickedColor, isCurrentlySaved: Boolean) {
        scope.launch {
            if (isCurrentlySaved) {
                dao.deleteSavedByArgb(pick.argb)
                analyticsTracker.logColorSaved(pick, action = "removed")
            } else {
                dao.upsertSaved(
                    SavedPickEntity(
                        argb = pick.argb,
                        name = pick.name,
                        updatedAt = System.currentTimeMillis()
                    )
                )
                dao.trimSaved(MAX)
                analyticsTracker.logColorSaved(pick, action = "saved")
                if (!firstSavedLogged) {
                    analyticsTracker.logFirstColorSaved(pick)
                    firstSavedLogged = true
                    prefs.edit().putBoolean(PREF_KEY_FIRST_SAVE_LOGGED, true).apply()
                }
            }
        }
    }

    suspend fun seedHistoryIfEmpty(picks: List<PickedColor>) {
        if (dao.loadHistory(MAX).isEmpty() && picks.isNotEmpty()) {
            val now = System.currentTimeMillis()
            val entities = picks.mapIndexed { index, pick ->
                RecentPickHistoryEntity(
                    argb = pick.argb,
                    name = pick.name,
                    updatedAt = now - index
                )
            }
            dao.insertHistory(entities)
            dao.trimHistory(MAX)
        }
    }

    suspend fun seedSavedIfEmpty(picks: List<PickedColor>) {
        if (dao.loadSaved(MAX).isEmpty() && picks.isNotEmpty()) {
            val now = System.currentTimeMillis()
            val entities = picks.mapIndexed { index, pick ->
                SavedPickEntity(
                    argb = pick.argb,
                    name = pick.name,
                    updatedAt = now - index
                )
            }
            dao.insertSaved(entities)
            dao.trimSaved(MAX)
        }
    }
}

