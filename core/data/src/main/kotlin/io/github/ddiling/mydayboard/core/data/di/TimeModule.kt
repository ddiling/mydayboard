package io.github.ddiling.mydayboard.core.data.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.ddiling.mydayboard.core.domain.id.IdGenerator
import io.github.ddiling.mydayboard.core.domain.id.UuidIdGenerator
import io.github.ddiling.mydayboard.core.domain.time.LiveZoneClock
import java.time.Clock
import javax.inject.Singleton

/** The only place the real clock and id source are chosen; tests pass their own through constructors. */
@Module
@InstallIn(SingletonComponent::class)
internal object TimeModule {
    @Provides
    @Singleton
    fun clock(): Clock = LiveZoneClock()

    @Provides
    fun ids(): IdGenerator = UuidIdGenerator()
}
