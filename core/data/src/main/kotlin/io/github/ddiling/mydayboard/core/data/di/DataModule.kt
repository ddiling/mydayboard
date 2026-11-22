package io.github.ddiling.mydayboard.core.data.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.ddiling.mydayboard.core.data.OfflineTaskCategoryRepository
import io.github.ddiling.mydayboard.core.data.OfflineTaskRepository
import io.github.ddiling.mydayboard.core.data.OfflineTaskTickRepository
import io.github.ddiling.mydayboard.core.data.OfflineTrackerReasonRepository
import io.github.ddiling.mydayboard.core.data.OfflineTrackerRepository
import io.github.ddiling.mydayboard.core.domain.task.TaskCategoryRepository
import io.github.ddiling.mydayboard.core.domain.task.TaskRepository
import io.github.ddiling.mydayboard.core.domain.task.TaskTickRepository
import io.github.ddiling.mydayboard.core.domain.tracker.TrackerReasonRepository
import io.github.ddiling.mydayboard.core.domain.tracker.TrackerRepository

@Module
@InstallIn(SingletonComponent::class)
internal interface DataModule {
    @Binds
    fun bindTrackerRepository(impl: OfflineTrackerRepository): TrackerRepository

    @Binds
    fun bindTrackerReasonRepository(impl: OfflineTrackerReasonRepository): TrackerReasonRepository

    @Binds
    fun bindTaskRepository(impl: OfflineTaskRepository): TaskRepository

    @Binds
    fun bindTaskCategoryRepository(impl: OfflineTaskCategoryRepository): TaskCategoryRepository

    @Binds
    fun bindTaskTickRepository(impl: OfflineTaskTickRepository): TaskTickRepository
}
