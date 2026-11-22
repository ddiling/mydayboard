package io.github.ddiling.mydayboard.core.testing

import io.github.ddiling.mydayboard.core.domain.id.IdGenerator
import io.github.ddiling.mydayboard.core.domain.task.TaskCategory
import io.github.ddiling.mydayboard.core.domain.task.TaskCategoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import java.time.Clock

/**
 * In-memory [TaskCategoryRepository] for ViewModel tests. It passes the same contract tests as the Room
 * implementation. Pass the [FakeTaskStore] of the [FakeTaskRepository] to use the same tasks.
 */
class FakeTaskCategoryRepository(
    private val clock: Clock,
    private val ids: IdGenerator,
    store: FakeTaskStore = FakeTaskStore(),
) : TaskCategoryRepository {
    private val state = store.state

    private val categoryOrder = compareBy<TaskCategory> { it.createdAt }.thenBy { it.id }

    override fun observeCategories(): Flow<List<TaskCategory>> =
        state.map { s -> s.categories.sortedWith(categoryOrder) }.distinctUntilChanged()

    override suspend fun createCategory(name: String): TaskCategory {
        val now = clock.instant()
        val category = TaskCategory(ids.newId(), name, createdAt = now, updatedAt = now)
        state.update { it.copy(categories = it.categories + category) }
        return category
    }

    override suspend fun renameCategory(
        id: String,
        name: String,
    ) {
        val now = clock.instant()
        state.update { s ->
            s.copy(categories = s.categories.map { if (it.id == id) it.copy(name = name, updatedAt = now) else it })
        }
    }

    override suspend fun deleteCategory(id: String) {
        state.update { s ->
            val gone = s.categories.firstOrNull { it.id == id } ?: return@update s
            s.copy(categories = s.categories - gone, deletedCategories = s.deletedCategories + gone)
        }
    }

    override suspend fun restoreCategory(id: String) {
        val now = clock.instant()
        state.update { s ->
            val back = s.deletedCategories.firstOrNull { it.id == id } ?: return@update s
            s.copy(
                categories = s.categories + back.copy(updatedAt = now),
                deletedCategories = s.deletedCategories - back,
            )
        }
    }
}
