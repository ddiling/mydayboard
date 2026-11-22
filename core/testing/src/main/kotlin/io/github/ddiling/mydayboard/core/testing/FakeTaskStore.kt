package io.github.ddiling.mydayboard.core.testing

import io.github.ddiling.mydayboard.core.domain.task.Task
import io.github.ddiling.mydayboard.core.domain.task.TaskCategory
import io.github.ddiling.mydayboard.core.domain.task.TaskTick
import kotlinx.coroutines.flow.MutableStateFlow

internal data class FakeTaskState(
    val tasks: List<Task> = emptyList(),
    val categories: List<TaskCategory> = emptyList(),
    val deletedCategories: List<TaskCategory> = emptyList(),
    val ticks: List<TaskTick> = emptyList(),
    val deletedTicks: List<TaskTick> = emptyList(),
)

/**
 * The data behind [FakeTaskRepository], [FakeTaskCategoryRepository] and [FakeTaskTickRepository]. They share
 * one store, like the one Room database behind the real ones: a task reads as uncategorised while its category
 * is deleted. Deleted categories and ticks are kept aside only so that a restore can bring them back.
 */
class FakeTaskStore {
    internal val state = MutableStateFlow(FakeTaskState())
}
