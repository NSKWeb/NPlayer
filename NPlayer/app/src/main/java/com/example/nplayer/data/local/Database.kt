package com.example.nplayer.data.local

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

/** Locally cached library item for instant, offline-friendly browsing. */
@Entity(tableName = "items")
data class CachedItem(
    @PrimaryKey val id: String,
    val name: String,
    val type: String?,
    val overview: String?,
    val productionYear: Int?,
    val runtimeMillis: Long?,
    val imageTag: String?,
    val seriesId: String?,
    val parentId: String?,
    val collectionType: String?,
    val resumePositionMillis: Long,
    val played: Boolean,
    val cachedAt: Long,
)

/** Everything the user has asked to keep offline. */
@Entity(tableName = "downloads")
data class DownloadEntity(
    @PrimaryKey val itemId: String,
    val name: String,
    val imageTag: String?,
    val mediaId: String,
    val state: Int,
    val progressPercent: Int,
    val bytesDownloaded: Long,
    val updatedAt: Long,
)

@Dao
interface LibraryDao {
    @Query("SELECT * FROM items ORDER BY cachedAt DESC")
    fun observeAll(): Flow<List<CachedItem>>

    @Query("SELECT * FROM items WHERE (name LIKE '%' || :query || '%') ORDER BY cachedAt DESC")
    fun observeSearch(query: String): Flow<List<CachedItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<CachedItem>)

    @Query("DELETE FROM items WHERE cachedAt < :cutoff AND id NOT IN (SELECT itemId FROM downloads)")
    suspend fun pruneOlderThan(cutoff: Long)
}

@Dao
interface DownloadDao {
    @Query("SELECT * FROM downloads ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE itemId = :itemId LIMIT 1")
    suspend fun find(itemId: String): DownloadEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: DownloadEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<DownloadEntity>)

    @Query("DELETE FROM downloads WHERE itemId = :itemId")
    suspend fun delete(itemId: String)
}

@Database(
    entities = [CachedItem::class, DownloadEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class NPlayerDatabase : RoomDatabase() {
    abstract fun libraryDao(): LibraryDao
    abstract fun downloadDao(): DownloadDao
}
