package com.primortex.color.service

import android.content.Context
import android.util.Log
import androidx.room.Room
import com.primortex.color.analytics.AnalyticsTracker
import com.primortex.color.app.PickedColor
import com.primortex.color.service.recentpicks.RecentPickHistoryEntity
import com.primortex.color.service.recentpicks.RecentPicksDatabase
import com.primortex.color.service.recentpicks.RecentPicksMetaEntity
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
import kotlinx.coroutines.runBlocking
import java.util.concurrent.Executors

@Singleton
class RecentPicksService @Inject constructor(
    @ApplicationContext context: Context,
    private val analyticsTracker: AnalyticsTracker
) {
    private companion object {
        const val MAX = 100
        const val META_KEY_FIRST_PICK_LOGGED = "first_pick_logged_v1"
        const val META_KEY_FIRST_SAVE_LOGGED = "first_saved_logged_v1"
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
    val history: StateFlow<List<PickedColor>> = dao.observeHistory(MAX)
        .map { rows -> rows.map { it.toPickedColor() } }
        .stateIn(scope, SharingStarted.Eagerly, emptyList())
    val saved: StateFlow<List<PickedColor>> = dao.observeSaved(MAX)
        .map { rows -> rows.map { it.toPickedColor() } }
        .stateIn(scope, SharingStarted.Eagerly, emptyList())
    private var firstPickLogged = false
    private var firstSavedLogged = false

    init {
        runBlocking(scope.coroutineContext) {
            firstPickLogged = dao.getMeta(META_KEY_FIRST_PICK_LOGGED) == "true"
            firstSavedLogged = dao.getMeta(META_KEY_FIRST_SAVE_LOGGED) == "true"
            Log.d("RecentPicksService", "Recent picks Room flow initialized")
        }
    }

    fun addPick(pick: PickedColor, source: String = "unknown") {
        analyticsTracker.logColorPicked(pick, source)
        if (!firstPickLogged) {
            analyticsTracker.logFirstColorPick(pick)
            firstPickLogged = true
        }
        runBlocking(scope.coroutineContext) {
            dao.upsertHistory(
                RecentPickHistoryEntity(
                    argb = pick.argb,
                    name = pick.name,
                    updatedAt = System.currentTimeMillis()
                )
            )
            dao.trimHistory(MAX)
            if (firstPickLogged) {
                dao.putMeta(RecentPicksMetaEntity(META_KEY_FIRST_PICK_LOGGED, "true"))
            }
        }
    }

    fun clear() {
        analyticsTracker.logRecentsCleared()
        runBlocking(scope.coroutineContext) {
            dao.clearHistory()
        }
    }

    fun clearSaved() {
        analyticsTracker.logSavedCleared()
        runBlocking(scope.coroutineContext) {
            dao.clearSaved()
        }
    }

    fun addSaved(pick: PickedColor) {
        runBlocking(scope.coroutineContext) {
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
                dao.putMeta(RecentPicksMetaEntity(META_KEY_FIRST_SAVE_LOGGED, "true"))
            }
        }
    }

    fun removeSaved(argb: Int) {
        runBlocking(scope.coroutineContext) {
            val removed = dao.getSavedByArgb(argb)?.toPickedColor()
            dao.deleteSavedByArgb(argb)
            removed?.let { analyticsTracker.logColorSaved(it, action = "removed") }
        }
    }

    fun toggleSaved(pick: PickedColor, isCurrentlySaved: Boolean) {
        runBlocking(scope.coroutineContext) {
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
                    dao.putMeta(RecentPicksMetaEntity(META_KEY_FIRST_SAVE_LOGGED, "true"))
                }
            }
        }
    }
}

