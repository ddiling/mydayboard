package io.github.ddiling.mydayboard.core.domain.task

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** How many calendar days the recent numbers (average, "X of N", skipped) look back. */
const val STATS_WINDOW_DAYS = 30

/**
 * Numbers about one task.
 *
 * One number, "credit" (done / target of an occurrence), feeds [doneOfPlanned], [creditPerDay] and the
 * streak, so the three can never disagree about what counts as a good day.
 */
data class TaskStats(
    val since: LocalDate,
    /** Settled occurrences done in a row, newest first; a planned day that was missed or half done ends it. */
    val streak: Int,
    /** Sum of credit over the settled occurrences in the window (a half day counts 0.5). */
    val doneOfPlanned: Double,
    /** Settled occurrences in the window; today's undone one is not settled yet, so it is not counted. */
    val plannedDays: Int,
    /** Credit divided by the calendar days of the window. */
    val creditPerDay: Double,
    /** Raw ticks (also above the goal) divided by the calendar days of the window. */
    val ticksPerDay: Double,
    /** Settled occurrences in the window with no credit at all; half days are not skipped. */
    val skipped: Int,
    /** Settled occurrences in the window that reached some of the target but not all of it. */
    val halfDays: Int,
    /** Days past the due day of an undone one-time task; null for other tasks, when done, or not yet late. */
    val daysLate: Int?,
)

/**
 * Statistics of [task] as of [today]. [until] lets an archived task stop at the day it was archived.
 *
 * An upcoming occurrence is never counted. A late occurrence counts as settled and missed, so a task left
 * undone cannot keep a streak alive. The window is [STATS_WINDOW_DAYS] calendar days of dates, so daylight
 * saving cannot change its length, and it starts no earlier than the task itself.
 */
fun computeTaskStats(
    task: Task,
    ticks: List<TaskTick>,
    today: LocalDate,
    until: LocalDate = today,
): TaskStats {
    val occurrences = occurrencesOf(task, ticks, today, until)
    val window = statsWindow(task, ticks, occurrences, today, until)
    return TaskStats(
        since = task.startDate,
        streak = if (task.repeat is Repeat.OneTime) 0 else streakOf(occurrences),
        doneOfPlanned = window.credit,
        plannedDays = window.settled.size,
        creditPerDay = window.credit.perDay(window.calendarDays),
        ticksPerDay = window.tickCount.toDouble().perDay(window.calendarDays),
        skipped = window.settled.count { it.done == 0 },
        halfDays = window.settled.count { it.done in 1 until it.target },
        daysLate = daysLateOf(task, occurrences, today),
    )
}

/** What one task contributes to the numbers of its window. */
internal data class StatsWindow(
    val from: LocalDate,
    val last: LocalDate,
    val settled: List<Occurrence>,
    val tickCount: Int,
) {
    val credit: Double get() = settled.sumOf { it.credit }
    val calendarDays: Int get() = maxOf(0, ChronoUnit.DAYS.between(from, last).toInt() + 1)
}

internal fun statsWindow(
    task: Task,
    ticks: List<TaskTick>,
    occurrences: List<Occurrence>,
    today: LocalDate,
    until: LocalDate,
): StatsWindow {
    val doneToday = occurrences.any { it.due == today && it.status == OccurrenceStatus.DONE }
    val last = if (doneToday) today else minOf(today.minusDays(1), until)
    val from = maxOf(task.startDate, last.minusDays(STATS_WINDOW_DAYS - 1L))
    val settled =
        occurrences.filter {
            it.isSettled() && !it.due.isBefore(from) && !it.due.isAfter(last)
        }
    val tickCount = ticks.count { !it.day.isBefore(from) && !it.day.isAfter(last) }
    return StatsWindow(from, last, settled, tickCount)
}

internal fun Double.perDay(days: Int): Double = if (days == 0) 0.0 else this / days

internal fun Occurrence.isSettled(): Boolean =
    status == OccurrenceStatus.DONE || status == OccurrenceStatus.ENDED || status == OccurrenceStatus.LATE

private fun streakOf(occurrences: List<Occurrence>): Int =
    occurrences
        .filter { it.isSettled() }
        .asReversed()
        .takeWhile { it.status == OccurrenceStatus.DONE }
        .size

private fun daysLateOf(
    task: Task,
    occurrences: List<Occurrence>,
    today: LocalDate,
): Int? {
    val oneTime = task.repeat as? Repeat.OneTime ?: return null
    val undone = occurrences.firstOrNull()?.status.let { it == OccurrenceStatus.LATE || it == OccurrenceStatus.ENDED }
    return if (undone) ChronoUnit.DAYS.between(oneTime.due, today).toInt() else null
}
