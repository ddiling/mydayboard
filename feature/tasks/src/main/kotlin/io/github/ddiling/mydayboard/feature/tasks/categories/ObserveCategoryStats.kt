package io.github.ddiling.mydayboard.feature.tasks.categories

import io.github.ddiling.mydayboard.core.domain.task.Task
import io.github.ddiling.mydayboard.core.domain.task.TaskCategoryRepository
import io.github.ddiling.mydayboard.core.domain.task.TaskRepository
import io.github.ddiling.mydayboard.core.domain.task.TaskTick
import io.github.ddiling.mydayboard.core.domain.task.TaskTickRepository
import io.github.ddiling.mydayboard.core.domain.task.computeCategoryStats
import io.github.ddiling.mydayboard.core.domain.task.computeTaskStats
import io.github.ddiling.mydayboard.core.domain.time.minuteTicker
import io.github.ddiling.mydayboard.core.domain.time.today
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.Clock
import java.time.Instant
import javax.inject.Inject

/**
 * The statistics of one category, rebuilt on every repository change and on every [ticker] emission, so the
 * numbers move at midnight and when the time zone changes. The day and the zone are read from [clock] at each
 * emission; the ticker is only a trigger. All maths is in `computeCategoryStats` and `computeTaskStats`, so the
 * screen cannot disagree with the details of a task. The ticker is a parameter so tests drive time themselves
 * (Hilt cannot use a default argument, so the production one lives in the `@Inject` constructor).
 */
internal class ObserveCategoryStats internal constructor(
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

    operator fun invoke(categoryId: String): Flow<CategoryStatsUiState> =
        combine(
            tasks.observeTasks(),
            ticks.observeAllTicks(),
            categories.observeCategories(),
            ticker(clock),
        ) { taskList, tickList, categoryList, _ ->
            val category = categoryList.firstOrNull { it.id == categoryId }
            val own = taskList.filter { it.categoryId == categoryId }
            when {
                category == null -> CategoryStatsUiState.NotFound
                own.isEmpty() -> CategoryStatsUiState.Empty(category.name)
                else -> content(category.name, own, tickList)
            }
        }

    private fun content(
        name: String,
        own: List<Task>,
        allTicks: List<TaskTick>,
    ): CategoryStatsUiState.Content {
        val today = clock.today()
        val stats = computeCategoryStats(own, allTicks, today, clock.zone)

        fun line(task: Task): TaskLine {
            val lastDay = task.archivedAt?.let { minOf(today, it.atZone(clock.zone).toLocalDate()) } ?: today
            val skipped = computeTaskStats(task, allTicks.filter { it.taskId == task.id }, today, lastDay).skipped
            return TaskLine(task.id, task.name, skipped)
        }
        val (archived, active) = own.partition { it.archivedAt != null }
        return CategoryStatsUiState.Content(
            categoryName = name,
            perDay = stats.creditPerDay,
            doneOfPlanned = stats.doneOfPlanned,
            plannedDays = stats.plannedDays,
            skipped = stats.skipped,
            tasks = active.map(::line),
            archivedLines = archived.map(::line),
        )
    }
}
