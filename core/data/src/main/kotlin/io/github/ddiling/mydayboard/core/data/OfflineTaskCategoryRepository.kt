package io.github.ddiling.mydayboard.core.data

import io.github.ddiling.mydayboard.core.database.dao.TaskCategoryDao
import io.github.ddiling.mydayboard.core.domain.id.IdGenerator
import io.github.ddiling.mydayboard.core.domain.task.TaskCategory
import io.github.ddiling.mydayboard.core.domain.task.TaskCategoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.time.Clock
import java.time.temporal.ChronoUnit
import javax.inject.Inject

/**
 * [TaskCategoryRepository] on Room. Deletes are soft (`deletedAt`), so a restore brings the category back at its
 * old place in the list, and unknown or deleted ids are no-ops. Times are cut to milliseconds, which is what the
 * database stores, so a created category equals what is read back.
 */
internal class OfflineTaskCategoryRepository
    @Inject
    constructor(
        private val dao: TaskCategoryDao,
        private val clock: Clock,
        private val ids: IdGenerator,
    ) : TaskCategoryRepository {
        override fun observeCategories(): Flow<List<TaskCategory>> =
            dao.observeLive().map { rows -> rows.map { it.toDomain() } }.distinctUntilChanged()

        override suspend fun createCategory(name: String): TaskCategory {
            val now = clock.instant().truncatedTo(ChronoUnit.MILLIS)
            val category = TaskCategory(ids.newId(), name, createdAt = now, updatedAt = now)
            dao.insert(category.toEntity())
            return category
        }

        override suspend fun renameCategory(
            id: String,
            name: String,
        ) {
            dao.renameIfLive(id, name, now = clock.millis())
        }

        override suspend fun deleteCategory(id: String) {
            dao.softDeleteIfLive(id, now = clock.millis())
        }

        override suspend fun restoreCategory(id: String) {
            dao.restoreIfDeleted(id, now = clock.millis())
        }
    }
