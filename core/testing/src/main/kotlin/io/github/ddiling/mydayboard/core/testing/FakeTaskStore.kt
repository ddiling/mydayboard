package io.github.ddiling.mydayboard.core.testing

import io.github.ddiling.mydayboard.core.domain.task.Task
import io.github.ddiling.mydayboard.core.domain.task.TaskCategory
import kotlinx.coroutines.flow.MutableStateFlow

internal data class FakeTaskState(
    val tasks: List<Task> = emptyList(),
    val categories: List<TaskCategory> = emptyList(),
    val deletedCategories: List<TaskCategory> = emptyList(),
)

/**
 * The data behind [FakeTaskRepository] and [FakeTaskCategoryRepository]. They share one store, like the one Room
 * database behind the real ones: a task reads as uncategorised while its category is deleted. Deleted
 * categories are kept aside only so that a restore can bring them back.
 */
class FakeTaskStore {
    internal val state = MutableStateFlow(FakeTaskState())
}
