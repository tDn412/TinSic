package com.tinsic.app.di

import com.tinsic.app.data.datasource.KaraokeDataSource
import com.tinsic.app.data.repository.KaraokeRepositoryImpl
import com.tinsic.app.domain.repository.KaraokeRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object KaraokeModule {

    @Provides
    @Singleton
    fun provideKaraokeRepository(
        karaokeDataSource: KaraokeDataSource
    ): KaraokeRepository {
        return KaraokeRepositoryImpl(karaokeDataSource)
    }

    // SpiceDetector, ScoringEngine, and KaraokeEngine are already provided via @Inject constructor
    // and Hilt will automatically generate the factories for them.
    // So we don't need manual @Provides for them unless we need to return an Interface or configure them specially.
}
