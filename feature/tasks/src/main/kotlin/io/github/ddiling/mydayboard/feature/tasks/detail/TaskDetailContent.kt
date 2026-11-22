package io.github.ddiling.mydayboard.feature.tasks.detail

import io.github.ddiling.mydayboard.core.domain.task.Occurrence
import io.github.ddiling.mydayboard.core.domain.task.OccurrenceStatus
import io.github.ddiling.mydayboard.core.domain.task.Repeat
import io.github.ddiling.mydayboard.core.domain.task.Task
import io.github.ddiling.mydayboard.core.domain.task.TaskStats
import io.github.ddiling.mydayboard.core.domain.task.TaskTick
import io.github.ddiling.mydayboard.core.domain.task.computeTaskStats
import io.github.ddiling.mydayboard.core.domain.task.nextDueDate
import io.github.ddiling.mydayboard.core.domain.task.occurrencesOf
import java.time.LocalDate

/**
 * Turns one task, its ticks and the day into what the details screen shows. The day is a parameter so the
 * caller reads the clock once per emission and everything here agrees on it.
 *
 * One occurrence decides hero, big button and chips: the one due today, or else a late one. Today's wins, as in
 * the list, so the two screens cannot show different states for the same task.
 */
internal fun detailContent(
    task: Task,
    categoryName: String?,
    ticks: List<TaskTick>,
    today: LocalDate,
): TaskDetailUiState.Content {
    val occurrences = occurrencesOf(task, ticks, today)
    val dueToday = occurrences.firstOrNull { it.due == today }
    val late = occurrences.firstOrNull { it.status == OccurrenceStatus.LATE }
    val todayTicks = ticks.filter { it.day == today }
    val open = dueToday?.takeIf { it.status == OccurrenceStatus.DUE_TODAY } ?: late
    val nextDue = if (dueToday == null && late == null) nextDueDate(task, ticks, today) else null
    return TaskDetailUiState.Content(
        task = task,
        categoryName = categoryName,
        today = today,
        hero = heroOf(task, dueToday, late, nextDue, occurrences.lastOrNull()),
        stats = statsOf(task, computeTaskStats(task, ticks, today)),
        chips = chipsOf(task, dueToday, todayTicks),
        history = occurrences.asReversed().mapNotNull { historyRowOf(task, it) },
        bigButton = bigButtonOf(task, open, todayTicks),
        nextDue = nextDue,
        pendingUndo = null,
    )
}

private fun heroOf(
    task: Task,
    dueToday: Occurrence?,
    late: Occurrence?,
    nextDue: LocalDate?,
    last: Occurrence?,
): Hero =
    when {
        dueToday?.status == OccurrenceStatus.DONE -> Hero.DoneToday
        dueToday != null -> progressHero(task, dueToday)
        late != null -> Hero.LateSince(late.due)
        nextDue != null -> Hero.NotDueToday
        else -> Hero.Finished(reached = last != null && last.done >= last.target)
    }

private fun progressHero(
    task: Task,
    occurrence: Occurrence,
): Hero =
    when {
        occurrence.done == 0 -> Hero.NotDone
        task.repeat is Repeat.TimesPerDay -> Hero.TimesProgress(occurrence.done, occurrence.target)
        else -> Hero.CountProgress(occurrence.done, occurrence.target)
    }

private fun chipsOf(
    task: Task,
    dueToday: Occurrence?,
    todayTicks: List<TaskTick>,
): List<TimeChip> {
    val times = (task.repeat as? Repeat.TimesPerDay)?.times
    return if (times == null || dueToday == null) {
        emptyList()
    } else {
        times.map { time -> TimeChip(time, ticked = todayTicks.any { it.slot == time }) }
    }
}

/** The one `when` on the occurrence: something open to do, something done today to take back, or nothing. */
private fun bigButtonOf(
    task: Task,
    open: Occurrence?,
    todayTicks: List<TaskTick>,
): BigButton =
    when {
        open != null -> openButton(task, todayTicks)
        todayTicks.isNotEmpty() -> BigButton.UntickToday
        else -> BigButton.None
    }

private fun openButton(
    task: Task,
    todayTicks: List<TaskTick>,
): BigButton {
    val repeat = task.repeat
    return when {
        repeat is Repeat.TimesPerDay -> {
            val ticked = todayTicks.map { it.slot }.toSet()
            repeat.times.firstOrNull { it !in ticked }?.let { BigButton.TickNext(it) } ?: BigButton.None
        }

        task.goal != null -> {
            BigButton.PlusOne
        }

        else -> {
            BigButton.MarkDone
        }
    }
}

/** Today's untouched or upcoming occurrence is not history yet; a late one is still open, so neither missed nor settled. */
private fun historyRowOf(
    task: Task,
    occurrence: Occurrence,
): HistoryRow? =
    when {
        occurrence.status == OccurrenceStatus.UPCOMING -> null
        occurrence.status == OccurrenceStatus.DONE -> HistoryRow(occurrence.due, HistoryResult.Done)
        occurrence.status == OccurrenceStatus.LATE -> HistoryRow(occurrence.due, HistoryResult.Late)
        occurrence.done == 0 && occurrence.status == OccurrenceStatus.DUE_TODAY -> null
        occurrence.done == 0 -> HistoryRow(occurrence.due, HistoryResult.Missed)
        else -> HistoryRow(occurrence.due, partialOf(task, occurrence))
    }

private fun partialOf(
    task: Task,
    occurrence: Occurrence,
) = HistoryResult.Partial(
    done = occurrence.done,
    target = occurrence.target,
    severalTimes = task.repeat is Repeat.TimesPerDay,
    settled = occurrence.status != OccurrenceStatus.DUE_TODAY,
)

private fun statsOf(
    task: Task,
    stats: TaskStats,
): DetailStats =
    if (task.repeat is Repeat.OneTime) {
        DetailStats.Facts(stats.since, stats.daysLate)
    } else {
        val countsTicks = task.goal != null
        DetailStats.Recurring(
            since = stats.since,
            streak = stats.streak,
            doneOfPlanned = stats.doneOfPlanned,
            plannedDays = stats.plannedDays,
            perDay = if (countsTicks) stats.ticksPerDay else stats.creditPerDay,
            countsTicks = countsTicks,
            skipped = stats.skipped,
            halfDays = if (task.repeat is Repeat.TimesPerDay) stats.halfDays else null,
        )
    }
