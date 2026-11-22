package io.github.ddiling.mydayboard.core.domain.task

import java.time.LocalDate

/** Where an [Occurrence] stands on a given day. */
enum class OccurrenceStatus {
    /** All of the target was ticked inside the window. */
    DONE,

    /** Due today and not done yet. */
    DUE_TODAY,

    /** Its due day has passed but the window is still open, so it can still be done. */
    LATE,

    /** The window closed before today without the target being reached; it counts as skipped. */
    ENDED,
}

/**
 * One planned due day of a task and how much of it is done.
 *
 * The window runs from [due] to [lastDay] (inclusive); [lastDay] is null while it has no end, so a late
 * occurrence stays open until it is done. One rule covers "late" and "missed": a window still open is late,
 * a window already closed is ended.
 */
data class Occurrence(
    val due: LocalDate,
    val lastDay: LocalDate?,
    val done: Int,
    val target: Int,
    val status: OccurrenceStatus,
) {
    /** Share of the target reached, 0.0 to 1.0. */
    val credit: Double get() = done.toDouble() / target
}

/**
 * The occurrences of [task] from its start date to [until], ascending by due day.
 *
 * Everything is computed on [LocalDate]s, so a daylight-saving change cannot add or drop a day; the caller
 * turns its clock into [today] and [until]. [until] is separate from [today] so an archived task can stop at
 * the day it was archived while its statuses are still judged against today.
 */
fun occurrencesOf(
    task: Task,
    ticks: List<TaskTick>,
    today: LocalDate,
    until: LocalDate = today,
): List<Occurrence> {
    val days = countedDays(task, ticks)
    return dueWindows(task, days)
        .takeWhile { !it.due.isAfter(until) }
        .map { occurrenceOf(task, it, days, today) }
        .toList()
}

/**
 * The next day [task] needs attention: the due day of a late or due-today occurrence, otherwise the next due
 * day after [today]. Null when nothing is left (a finished one-time task).
 */
fun nextDueDate(
    task: Task,
    ticks: List<TaskTick>,
    today: LocalDate,
): LocalDate? {
    val days = countedDays(task, ticks)
    return dueWindows(task, days)
        .map { occurrenceOf(task, it, days, today) }
        .firstOrNull {
            it.due.isAfter(today) || it.status == OccurrenceStatus.LATE || it.status == OccurrenceStatus.DUE_TODAY
        }?.due
}

/** Days of the ticks that count. A several-times task counts one tick per current time; others count all. */
internal fun countedDays(
    task: Task,
    ticks: List<TaskTick>,
): List<LocalDate> {
    val repeat = task.repeat
    return if (repeat is Repeat.TimesPerDay) {
        ticks
            .filter { it.slot in repeat.times }
            .distinctBy { it.day to it.slot }
            .map { it.day }
    } else {
        ticks.map { it.day }
    }.sorted()
}

/** How many ticks complete one occurrence. */
internal fun targetOf(task: Task): Int =
    when (val repeat = task.repeat) {
        is Repeat.TimesPerDay -> repeat.times.size
        else -> task.goal ?: 1
    }

private fun occurrenceOf(
    task: Task,
    window: DueWindow,
    days: List<LocalDate>,
    today: LocalDate,
): Occurrence {
    val target = targetOf(task)
    val done = days.count { !it.isBefore(window.due) && (window.lastDay == null || !it.isAfter(window.lastDay)) }
    val capped = minOf(done, target)
    val open = window.lastDay == null || !window.lastDay.isBefore(today)
    val status =
        when {
            capped == target -> OccurrenceStatus.DONE
            !open -> OccurrenceStatus.ENDED
            window.due.isBefore(today) -> OccurrenceStatus.LATE
            else -> OccurrenceStatus.DUE_TODAY
        }
    return Occurrence(window.due, window.lastDay, capped, target, status)
}

/** A due day and the last day it can still be done; null means no end yet. */
internal data class DueWindow(
    val due: LocalDate,
    val lastDay: LocalDate?,
)
