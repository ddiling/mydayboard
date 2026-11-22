package io.github.ddiling.mydayboard.core.domain.task

import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/** Numbers about the tasks of one category, shown as "tasks done a day". */
data class CategoryStats(
    val taskCount: Int,
    /** Sum of the tasks' credit divided by the calendar days of the window. */
    val creditPerDay: Double,
    val doneOfPlanned: Double,
    val plannedDays: Int,
    val skipped: Int,
)

/**
 * Statistics of [tasks] together as of [today], over ONE window shared by all tasks: the last
 * [STATS_WINDOW_DAYS] calendar days ending yesterday (or today when a task is due today and every task due
 * today is done, like the window of a single task), and starting no earlier than the earliest start of
 * the tasks, so a young category is not averaged over days before any task existed. Each task adds only its
 * days inside that window and up to its own last day (the day it was archived in [zone]), so a task archived
 * before the window adds nothing and old history never leaks into the recent average. The average is per
 * day of that window. The caller leaves out the tasks of a deleted category.
 */
fun computeCategoryStats(
    tasks: List<Task>,
    ticks: List<TaskTick>,
    today: LocalDate,
    zone: ZoneId,
): CategoryStats {
    val occurrences =
        tasks.map { task ->
            val own = ticks.filter { it.taskId == task.id }
            occurrencesOf(task, own, today, lastDayOf(task, today, zone))
        }
    val dueToday = occurrences.flatten().filter { it.due == today }
    val last =
        if (dueToday.isNotEmpty() && dueToday.all { it.status == OccurrenceStatus.DONE }) today else today.minusDays(1)
    val earliestStart = tasks.minOfOrNull { it.startDate }
    val from = earliestStart?.let { maxOf(it, last.minusDays(STATS_WINDOW_DAYS - 1L)) }
    val settled =
        if (from == null) {
            emptyList()
        } else {
            occurrences.flatten().filter { it.isSettled() && !it.due.isBefore(from) && !it.due.isAfter(last) }
        }
    val days = if (from == null) 0 else maxOf(0, ChronoUnit.DAYS.between(from, last).toInt() + 1)
    val credit = settled.sumOf { it.credit }
    return CategoryStats(
        taskCount = tasks.size,
        creditPerDay = credit.perDay(days),
        doneOfPlanned = credit,
        plannedDays = settled.size,
        skipped = settled.count { it.done == 0 },
    )
}

private fun lastDayOf(
    task: Task,
    today: LocalDate,
    zone: ZoneId,
): LocalDate = task.archivedAt?.let { minOf(today, it.atZone(zone).toLocalDate()) } ?: today
