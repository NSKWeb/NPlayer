package com.example.nplayer.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.nplayer.data.local.CachedItem
import com.example.nplayer.data.local.LibraryDao
import com.example.nplayer.data.repository.ApiResult
import com.example.nplayer.data.repository.LibraryRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Periodically refreshes a lightweight snapshot of the user's library into Room
 * so the app opens instantly and can browse previously-seen items offline.
 */
@HiltWorker
class LibrarySyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val libraryRepository: LibraryRepository,
    private val libraryDao: LibraryDao,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        if (!libraryRepository.isAuthenticated()) return Result.success()

        return when (val result = libraryRepository.latest(limit = 60)) {
            is ApiResult.Success -> {
                val now = System.currentTimeMillis()
                val cached = result.data.items.map { item ->
                    CachedItem(
                        id = item.id,
                        name = item.name ?: "",
                        type = item.type,
                        overview = item.overview,
                        productionYear = item.productionYear,
                        runtimeMillis = item.runtimeMillis,
                        imageTag = item.primaryImageTag,
                        seriesId = item.seriesId,
                        parentId = null,
                        collectionType = item.collectionType,
                        resumePositionMillis = item.userData?.resumePositionMillis ?: 0L,
                        played = item.userData?.played ?: false,
                        cachedAt = now,
                    )
                }
                libraryDao.upsertAll(cached)
                libraryDao.pruneOlderThan(now - PRUNE_WINDOW_MILLIS)
                Result.success()
            }
            is ApiResult.Failure -> Result.retry()
        }
    }

    private companion object {
        const val PRUNE_WINDOW_MILLIS = 30L * 24 * 60 * 60 * 1000
    }
}
