package io.github.ddiling.mydayboard.core.domain.task

import java.time.LocalTime

private const val DEFAULT_DASHBOARD_LIMIT = 3

private val byTimeThenName: Comparator<TaskRow> =
    compareBy<TaskRow, LocalTime?>(nullsLast()) { it.time }
        .thenBy(String.CASE_INSENSITIVE_ORDER) { it.task.name }
        .thenBy { it.task.id }

private val byNameThenId: Comparator<TaskRow> =
    compareBy<TaskRow, String>(String.CASE_INSENSITIVE_ORDER) { it.task.name }.thenBy { it.task.id }

private fun TaskRow.isDueToday(): Boolean = state == RowState.PENDING || state == RowState.DONE

private val dueTodayFirst: Comparator<TaskRow> =
    Comparator { a, b ->
        when {
            a.isDueToday() && b.isDueToday() -> byTimeThenName.compare(a, b)
            a.isDueToday() -> -1
            b.isDueToday() -> 1
            else -> byNameThenId.compare(a, b)
        }
    }

private val byNameThenCategoryId: Comparator<TaskCategory> =
    compareBy<TaskCategory, String>(String.CASE_INSENSITIVE_ORDER) { it.name }.thenBy { it.id }

/**
 * The undone tasks of today for the dashboard, soonest first, at most [limit]. Late rows are left out on
 * purpose: the dashboard shows today, and a late stays task would crowd it out. Rows without a time come after
 * timed ones, by name ignoring case. Ties go by task id, so the order never depends on the repository.
 */
fun TasksOverview.dashboardRows(limit: Int = DEFAULT_DASHBOARD_LIMIT): List<TaskRow> =
    rows.filter { it.state == RowState.PENDING }.sortedWith(byTimeThenName).take(limit)

/**
 * The full list by category: groups by name ignoring case, then by category id (names that differ only by case
 * stay separate groups), "No category" (null) last. A task whose category is gone lands in "No category".
 * Inside a group, rows due today (pending, done) come first by time (no time last), then name ignoring case,
 * then id. The other rows (late, upcoming) follow by name ignoring case, then id, not by time: their time is
 * not today's.
 */
fun TasksOverview.groupedByCategory(): List<TaskGroup> {
    val byCategory = rows.groupBy { it.category }
    val named =
        byCategory.keys
            .filterNotNull()
            .sortedWith(byNameThenCategoryId)
    return (named + null).mapNotNull { category ->
        byCategory[category]?.let { TaskGroup(category, it.sortedWith(dueTodayFirst)) }
    }
}
