package io.github.ddiling.mydayboard.core.domain.task

import java.time.LocalTime

private const val DEFAULT_DASHBOARD_LIMIT = 3

private val byTimeThenName: Comparator<TaskRow> =
    compareBy<TaskRow, LocalTime?>(nullsLast()) { it.time }
        .thenBy(String.CASE_INSENSITIVE_ORDER) { it.task.name }
        .thenBy { it.task.id }

private val dueTodayFirst: Comparator<TaskRow> =
    compareBy<TaskRow> { it.state != RowState.PENDING && it.state != RowState.DONE }.then(byTimeThenName)

/**
 * The undone tasks of today for the dashboard, soonest first, at most [limit]. Late rows are left out on
 * purpose: the dashboard shows today, and a late stays task would crowd it out. Rows without a time come after
 * timed ones, by name ignoring case.
 */
fun TasksOverview.dashboardRows(limit: Int = DEFAULT_DASHBOARD_LIMIT): List<TaskRow> =
    rows.filter { it.state == RowState.PENDING }.sortedWith(byTimeThenName).take(limit)

/**
 * The full list by category: groups by name ignoring case, "No category" (null) last. A task whose category is
 * gone lands in "No category". Inside a group, tasks due today come first by time, then the rest by name.
 */
fun TasksOverview.groupedByCategory(): List<TaskGroup> {
    val byCategory = rows.groupBy { it.category }
    val named =
        byCategory.keys
            .filterNotNull()
            .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })
    return (named + null).mapNotNull { category ->
        byCategory[category]?.let { TaskGroup(category, it.sortedWith(dueTodayFirst)) }
    }
}
