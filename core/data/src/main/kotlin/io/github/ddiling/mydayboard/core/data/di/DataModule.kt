package io.github.ddiling.mydayboard.core.data.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.ddiling.mydayboard.core.data.OfflineTrackerRepository
import io.github.ddiling.mydayboard.core.domain.tracker.TrackerRepository

@Module
@InstallIn(SingletonComponent::class)
internal interface DataModule {
    @Binds
    fun bindTrackerRepository(impl: OfflineTrackerRepository): TrackerRepository
}
