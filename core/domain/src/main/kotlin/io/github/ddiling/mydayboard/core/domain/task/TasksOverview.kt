package io.github.ddiling.mydayboard.core.domain.task

import java.time.LocalDate
import java.time.LocalTime

/** What a task needs from the user on the overview's day. */
enum class RowState {
    /** Due today and not done yet. */
    PENDING,

    /** Due today and done. */
    DONE,

    /** A stays task whose due day has passed and which can still be done. Never shown on the dashboard. */
    LATE,

    /**
     * Not due today; [TaskRow.nextDue] says when it is next due. It is null when nothing is left, for example a
     * finished one-time task: it stays in the list so the user can still open, untick or archive it, and the UI
     * shows it as done.
     */
    UPCOMING,
}

/**
 * One live task as the list and the dashboard show it.
 *
 * [time] is the time the row sorts and displays by: the task's own time, or for a several-times task the next
 * time not yet done today (its first time once all are done). [done] and [target] are of the occurrence the
 * row is about, so "1 of 2" and a count goal read the same way; both are 0 for an upcoming row.
 */
data class TaskRow(
    val task: Task,
    val category: TaskCategory?,
    val state: RowState,
    val done: Int,
    val target: Int,
    val time: LocalTime?,
    val nextDue: LocalDate?,
    val lateSince: LocalDate?,
)

/**
 * Everything the task list and the dashboard show, computed once so they cannot disagree. [today] is the local
 * date the rows were judged on. Archived tasks are not in [rows]; they only add to [archivedCount].
 */
data class TasksOverview(
    val today: LocalDate,
    val rows: List<TaskRow>,
    val archivedCount: Int,
) {
    /** Tasks due today, done or not. Late and upcoming rows are not part of today's progress. */
    val totalToday: Int get() = rows.count { it.state == RowState.PENDING || it.state == RowState.DONE }

    val doneToday: Int get() = rows.count { it.state == RowState.DONE }
}

/** A category and its rows in the full list; [category] is null for the "No category" group. */
data class TaskGroup(
    val category: TaskCategory?,
    val rows: List<TaskRow>,
)
