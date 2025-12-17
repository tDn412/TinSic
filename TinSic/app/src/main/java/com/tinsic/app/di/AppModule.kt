package com.tinsic.app.di

import android.content.Context
import androidx.media3.exoplayer.ExoPlayer

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
    fun provideDatabaseProvider(@ApplicationContext context: Context): androidx.media3.database.DatabaseProvider {
        return androidx.media3.database.StandaloneDatabaseProvider(context)
    }

    @Provides
    @Singleton
    fun provideCache(
        @ApplicationContext context: Context,
        databaseProvider: androidx.media3.database.DatabaseProvider
    ): androidx.media3.datasource.cache.Cache {
        val cacheEvictor = androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor(1024 * 1024 * 500) // 500MB
        val cacheDir = java.io.File(context.cacheDir, "media_cache")
        return androidx.media3.datasource.cache.SimpleCache(cacheDir, cacheEvictor, databaseProvider)
    }

    @Provides
    @Singleton
    fun provideExoPlayer(
        @ApplicationContext context: Context,
        cache: androidx.media3.datasource.cache.Cache
    ): ExoPlayer {
        // Build CacheDataSource Factory
        val cacheDataSourceFactory = androidx.media3.datasource.cache.CacheDataSource.Factory()
            .setCache(cache)
            .setUpstreamDataSourceFactory(androidx.media3.datasource.DefaultHttpDataSource.Factory())
            .setFlags(androidx.media3.datasource.cache.CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)

        val mediaSourceFactory = androidx.media3.exoplayer.source.DefaultMediaSourceFactory(cacheDataSourceFactory)

        return ExoPlayer.Builder(context)
            .setMediaSourceFactory(mediaSourceFactory)
            .build()
    }
}
