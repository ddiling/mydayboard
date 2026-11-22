package io.github.ddiling.mydayboard.core.data

import io.github.ddiling.mydayboard.core.database.dao.TaskCategoryDao
import io.github.ddiling.mydayboard.core.database.dao.TaskDao
import io.github.ddiling.mydayboard.core.database.model.TaskEntity
import io.github.ddiling.mydayboard.core.domain.id.IdGenerator
import io.github.ddiling.mydayboard.core.domain.task.Task
import io.github.ddiling.mydayboard.core.domain.task.TaskDraft
import io.github.ddiling.mydayboard.core.domain.task.TaskRepository
import io.github.ddiling.mydayboard.core.domain.task.normalized
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.time.Clock
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.inject.Inject

/**
 * [TaskRepository] on Room. Deletes are soft and unknown or deleted ids are no-ops, because every write is one
 * conditional statement in the DAO.
 *
 * A task keeps its category id when the category is deleted (so an Undo can restore it), but the rule that it
 * then reads as uncategorised lives here, so no screen has to remember it. That is why the task flows are
 * combined with the live categories. [distinctUntilChanged] stops a category write that changes no task from
 * waking collectors.
 */
internal class OfflineTaskRepository
    @Inject
    constructor(
        private val taskDao: TaskDao,
        private val categoryDao: TaskCategoryDao,
        private val clock: Clock,
        private val ids: IdGenerator,
    ) : TaskRepository {
        override fun observeTasks(): Flow<List<Task>> =
            combine(taskDao.observeLive(), liveCategoryIds()) { rows, live ->
                rows.map { it.toDomain().withLiveCategory(live) }
            }.distinctUntilChanged()

        override fun observeTask(id: String): Flow<Task?> =
            combine(taskDao.observeLiveById(id), liveCategoryIds()) { row, live ->
                row?.toDomain()?.withLiveCategory(live)
            }.distinctUntilChanged()

        override suspend fun createTask(
            draft: TaskDraft,
            startDate: LocalDate,
        ): Task {
            // Room stores epoch millis, so the returned task must not carry finer precision than what is read back.
            val now = clock.instant().truncatedTo(ChronoUnit.MILLIS)
            val task =
                draft.normalized().toTask(
                    ids.newId(),
                    startDate,
                    archivedAt = null,
                    createdAt = now,
                    updatedAt = now,
                )
            taskDao.insert(task.toEntity())
            return task
        }

        override suspend fun updateTask(
            id: String,
            draft: TaskDraft,
        ) {
            val stored: TaskEntity = taskDao.findLive(id) ?: return
            val now = clock.instant()
            val task =
                draft.normalized().toTask(
                    id,
                    LocalDate.ofEpochDay(stored.startDate),
                    archivedAt = null,
                    now,
                    now,
                )
            // The DAO keeps createdAt and archivedAt of the stored row and ignores a concurrent delete.
            taskDao.updateIfLive(task.toEntity(), now.toEpochMilli())
        }

        override suspend fun archiveTask(id: String) {
            // One reading, so that archivedAt and updatedAt are the same instant.
            val now = clock.millis()
            taskDao.setArchivedAt(id, archivedAt = now, now = now)
        }

        override suspend fun restoreTask(id: String) {
            taskDao.setArchivedAt(id, archivedAt = null, now = clock.millis())
        }

        override suspend fun deleteTask(id: String) {
            taskDao.softDeleteIfLive(id, now = clock.millis())
        }

        private fun liveCategoryIds(): Flow<Set<String>> =
            categoryDao.observeLive().map { rows -> rows.map { it.id }.toSet() }.distinctUntilChanged()

        private fun Task.withLiveCategory(live: Set<String>): Task =
            if (categoryId == null || categoryId in live) this else copy(categoryId = null)
    }
