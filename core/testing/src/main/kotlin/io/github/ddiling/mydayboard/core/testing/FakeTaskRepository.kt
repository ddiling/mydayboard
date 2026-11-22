package io.github.ddiling.mydayboard.core.testing

import io.github.ddiling.mydayboard.core.domain.id.IdGenerator
import io.github.ddiling.mydayboard.core.domain.task.Task
import io.github.ddiling.mydayboard.core.domain.task.TaskDraft
import io.github.ddiling.mydayboard.core.domain.task.TaskRepository
import io.github.ddiling.mydayboard.core.domain.task.normalized
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import java.time.Clock
import java.time.LocalDate

/**
 * In-memory [TaskRepository] for ViewModel tests. It passes the same contract tests as the Room implementation.
 * Pass the [FakeTaskStore] of the [FakeTaskCategoryRepository] to use the same categories.
 */
class FakeTaskRepository(
    private val clock: Clock,
    private val ids: IdGenerator,
    store: FakeTaskStore = FakeTaskStore(),
) : TaskRepository {
    private val state = store.state

    private val taskOrder = compareBy<Task> { it.createdAt }.thenBy { it.id }

    override fun observeTasks(): Flow<List<Task>> =
        state
            .map { s -> s.tasks.sortedWith(taskOrder).map { it.withLiveCategory(s) } }
            .distinctUntilChanged()

    override fun observeTask(id: String): Flow<Task?> =
        state.map { s -> s.tasks.firstOrNull { it.id == id }?.withLiveCategory(s) }.distinctUntilChanged()

    override suspend fun createTask(
        draft: TaskDraft,
        startDate: LocalDate,
    ): Task {
        val now = clock.instant()
        val task =
            with(draft.normalized()) {
                Task(ids.newId(), name, categoryId, repeat, time, note, goal, onMiss, startDate, null, now, now)
            }
        state.update { it.copy(tasks = it.tasks + task) }
        return task
    }

    override suspend fun updateTask(
        id: String,
        draft: TaskDraft,
    ) {
        val now = clock.instant()
        change(id) {
            with(draft.normalized()) {
                it.copy(
                    name = name,
                    categoryId = categoryId,
                    repeat = repeat,
                    time = time,
                    note = note,
                    goal = goal,
                    onMiss = onMiss,
                    updatedAt = now,
                )
            }
        }
    }

    override suspend fun archiveTask(id: String) {
        val now = clock.instant()
        change(id) { it.copy(archivedAt = now, updatedAt = now) }
    }

    override suspend fun restoreTask(id: String) {
        val now = clock.instant()
        change(id) { it.copy(archivedAt = null, updatedAt = now) }
    }

    override suspend fun deleteTask(id: String) {
        state.update { s -> s.copy(tasks = s.tasks.filterNot { it.id == id }) }
    }

    private fun change(
        id: String,
        edit: (Task) -> Task,
    ) = state.update { s -> s.copy(tasks = s.tasks.map { if (it.id == id) edit(it) else it }) }

    private fun Task.withLiveCategory(s: FakeTaskState): Task =
        if (categoryId == null || s.categories.any { it.id == categoryId }) this else copy(categoryId = null)
}
