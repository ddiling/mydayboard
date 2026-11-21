package io.github.ddiling.mydayboard.feature.trackers.reason

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlin.random.Random

/** Provides the random source of [ReasonPrompter], so tests can pass a seeded one. */
@Module
@InstallIn(SingletonComponent::class)
internal object ReasonModule {
    @Provides
    fun provideRandom(): Random = Random.Default
}
