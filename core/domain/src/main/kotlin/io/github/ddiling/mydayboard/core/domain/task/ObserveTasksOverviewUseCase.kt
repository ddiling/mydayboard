package io.github.ddiling.mydayboard.core.domain.task

import io.github.ddiling.mydayboard.core.domain.time.minuteTicker
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject

/**
 * The one place that turns tasks, categories, ticks and time into the overview, so the list and the dashboard
 * cannot disagree. It recomputes on every repository change and on every [ticker] emission, which lets "today"
 * roll over at midnight without a data change.
 *
 * The ticker is only a trigger: the date is always read from [clock] (instant and zone) at the emission. A tick
 * added just after a ticker emission is then judged on the real current day, not on a value up to a minute old.
 * A time zone change is picked up at the next emission.
 *
 * The ticker is a constructor parameter so tests can drive time themselves; Hilt cannot use a Kotlin default
 * argument, so the production default lives in a separate `@Inject` constructor.
 */
class ObserveTasksOverviewUseCase internal constructor(
    private val taskRepository: TaskRepository,
    private val categoryRepository: TaskCategoryRepository,
    private val tickRepository: TaskTickRepository,
    private val clock: Clock,
    private val ticker: (Clock) -> Flow<Instant>,
) {
    @Inject
    constructor(
        taskRepository: TaskRepository,
        categoryRepository: TaskCategoryRepository,
        tickRepository: TaskTickRepository,
        clock: Clock,
    ) : this(taskRepository, categoryRepository, tickRepository, clock, ::minuteTicker)

    operator fun invoke(): Flow<TasksOverview> =
        combine(
            taskRepository.observeTasks(),
            categoryRepository.observeCategories(),
            tickRepository.observeAllTicks(),
            ticker(clock),
        ) { tasks, categories, ticks, _ ->
            val today = clock.instant().atZone(clock.zone).toLocalDate()
            buildTasksOverview(tasks, categories, ticks, today)
        }
}

internal fun buildTasksOverview(
    tasks: List<Task>,
    categories: List<TaskCategory>,
    ticks: List<TaskTick>,
    today: LocalDate,
): TasksOverview {
    val (archived, live) = tasks.partition { it.archivedAt != null }
    val categoriesById = categories.associateBy { it.id }
    val ticksByTask = ticks.groupBy { it.taskId }
    val rows =
        live.map { task ->
            rowOf(task, categoriesById[task.categoryId], ticksByTask[task.id].orEmpty(), today)
        }
    return TasksOverview(today, rows, archived.size)
}

private fun rowOf(
    task: Task,
    category: TaskCategory?,
    ticks: List<TaskTick>,
    today: LocalDate,
): TaskRow {
    val occurrences = occurrencesOf(task, ticks, today)
    val dueToday = occurrences.firstOrNull { it.due == today }
    val late = occurrences.firstOrNull { it.status == OccurrenceStatus.LATE }
    return when {
        dueToday != null -> {
            val state = if (dueToday.status == OccurrenceStatus.DONE) RowState.DONE else RowState.PENDING
            TaskRow(task, category, state, dueToday.done, dueToday.target, displayTime(task, ticks, today), today, null)
        }

        late != null -> {
            TaskRow(task, category, RowState.LATE, late.done, late.target, task.time, late.due, late.due)
        }

        else -> {
            TaskRow(task, category, RowState.UPCOMING, 0, 0, task.time, nextDueDate(task, ticks, today), null)
        }
    }
}

/** A several-times task is ordered by its next time not done today, so it moves down the list as it is ticked. */
private fun displayTime(
    task: Task,
    ticks: List<TaskTick>,
    today: LocalDate,
) = when (val repeat = task.repeat) {
    is Repeat.TimesPerDay -> {
        val doneSlots = ticks.filter { it.day == today }.map { it.slot }.toSet()
        repeat.times.firstOrNull { it !in doneSlots } ?: repeat.times.first()
    }

    else -> {
        task.time
    }
}
