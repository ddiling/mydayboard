package io.github.ddiling.mydayboard.core.domain.task

import java.time.LocalDate
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
 * Statistics of [tasks] together as of [today]; archived tasks are included so archiving does not rewrite
 * history. Days are counted from the earliest start inside the window, so a task added later never lowers
 * the average of the days before it existed. The caller leaves out the tasks of a deleted category.
 */
fun computeCategoryStats(
    tasks: List<Task>,
    ticks: List<TaskTick>,
    today: LocalDate,
): CategoryStats {
    val windows =
        tasks.map { task ->
            val own = ticks.filter { it.taskId == task.id }
            statsWindow(task, own, occurrencesOf(task, own, today), today, today)
        }
    val from = windows.minOfOrNull { it.from }
    val last = windows.maxOfOrNull { it.last }
    val days = if (from == null || last == null) 0 else maxOf(0, ChronoUnit.DAYS.between(from, last).toInt() + 1)
    val credit = windows.sumOf { it.credit }
    return CategoryStats(
        taskCount = tasks.size,
        creditPerDay = credit.perDay(days),
        doneOfPlanned = credit,
        plannedDays = windows.sumOf { it.settled.size },
        skipped = windows.sumOf { w -> w.settled.count { it.done == 0 } },
    )
}
