package io.github.ddiling.mydayboard.feature.tasks.detail

import io.github.ddiling.mydayboard.core.domain.task.TaskCategoryRepository
import io.github.ddiling.mydayboard.core.domain.task.TaskRepository
import io.github.ddiling.mydayboard.core.domain.task.TaskTickRepository
import io.github.ddiling.mydayboard.core.domain.time.minuteTicker
import io.github.ddiling.mydayboard.core.domain.time.today
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.Clock
import java.time.Instant
import javax.inject.Inject

/**
 * The details of one task, rebuilt on every repository change and on every [ticker] emission, so the status word
 * resets at midnight and an untick takes the stats back in the same flow. The day is read from [clock] at each
 * emission; the ticker is only a trigger. It is a parameter so tests drive time themselves (Hilt cannot use a
 * default argument, so the production one lives in the `@Inject` constructor).
 *
 * A deleted or archived task reads as [TaskDetailUiState.NotFound]: archived tasks have their own screen mode.
 */
internal class ObserveTaskDetail internal constructor(
    private val tasks: TaskRepository,
    private val ticks: TaskTickRepository,
    private val categories: TaskCategoryRepository,
    private val clock: Clock,
    private val ticker: (Clock) -> Flow<Instant>,
) {
    @Inject
    constructor(
        tasks: TaskRepository,
        ticks: TaskTickRepository,
        categories: TaskCategoryRepository,
        clock: Clock,
    ) : this(tasks, ticks, categories, clock, ::minuteTicker)

    operator fun invoke(taskId: String): Flow<TaskDetailUiState> =
        combine(
            tasks.observeTask(taskId),
            ticks.observeTicks(taskId),
            categories.observeCategories(),
            ticker(clock),
        ) { task, tickList, categoryList, _ ->
            if (task == null || task.archivedAt != null) {
                TaskDetailUiState.NotFound
            } else {
                val category = categoryList.firstOrNull { it.id == task.categoryId }
                detailContent(task, category?.name, tickList, clock.today())
            }
        }
}
