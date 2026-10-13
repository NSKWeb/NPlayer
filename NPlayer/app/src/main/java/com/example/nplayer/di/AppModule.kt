package com.example.nplayer.di

import android.content.Context
import androidx.room.Room
import com.example.nplayer.data.local.DownloadDao
import com.example.nplayer.data.local.LibraryDao
import com.example.nplayer.data.local.NPlayerDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): NPlayerDatabase =
        Room.databaseBuilder(context, NPlayerDatabase::class.java, "nplayer.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideLibraryDao(db: NPlayerDatabase): LibraryDao = db.libraryDao()

    @Provides
    fun provideDownloadDao(db: NPlayerDatabase): DownloadDao = db.downloadDao()
}
