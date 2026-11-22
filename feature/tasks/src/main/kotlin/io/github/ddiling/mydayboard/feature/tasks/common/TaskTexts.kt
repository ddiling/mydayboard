package io.github.ddiling.mydayboard.feature.tasks.common

import android.content.res.Resources
import io.github.ddiling.mydayboard.core.domain.task.Repeat
import io.github.ddiling.mydayboard.core.domain.task.RowState
import io.github.ddiling.mydayboard.core.domain.task.Task
import io.github.ddiling.mydayboard.core.domain.task.TaskRow
import io.github.ddiling.mydayboard.feature.tasks.R
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private const val SEPARATOR = " · "
private const val DAYS_IN_WEEK = 7
private const val TENS_CYCLE = 10
private const val TEENS_CYCLE = 100
private const val FIRST_TEEN = 11
private const val LAST_TEEN = 13
private val timeFormat = DateTimeFormatter.ofPattern("HH:mm")
private val workWeek =
    setOf(
        DayOfWeek.MONDAY,
        DayOfWeek.TUESDAY,
        DayOfWeek.WEDNESDAY,
        DayOfWeek.THURSDAY,
        DayOfWeek.FRIDAY,
    )

private fun Resources.locale(): Locale = configuration.locales[0]

/**
 * When the task repeats, as one short phrase: "08:30", "Mon, Wed, Fri", "every 3 days", "every weekday". A time
 * is added after the phrase; for every-day and several-times tasks the times are the whole phrase, because
 * "every day · 08:30" only repeats what the list already says. The text is built here, not stored, so it
 * follows the device language.
 */
internal fun scheduleText(
    resources: Resources,
    task: Task,
): String {
    val time = task.time?.format(timeFormat)
    return when (val repeat = task.repeat) {
        is Repeat.TimesPerDay -> repeat.times.joinToString(", ") { it.format(timeFormat) }
        Repeat.EveryDay -> time ?: resources.getString(R.string.tasks_schedule_every_day)
        else -> listOfNotNull(repeatText(resources, repeat), time).joinToString(SEPARATOR)
    }
}

/**
 * The grey line under a task's name in the list. A late task says since when ("late · was due Fri"). A task with
 * nothing left to do says "Done" or "Missed", so a finished one-time task still tells what happened. Otherwise
 * it is the schedule; for "every N days" and monthly tasks the weekday of the next due day follows, because
 * those dates cannot be guessed from the phrase. [today] decides whether a late day is shown as a weekday
 * (within the last week, where it is unambiguous) or as a date.
 */
internal fun detailsText(
    resources: Resources,
    row: TaskRow,
    today: LocalDate,
): String {
    val locale = resources.locale()
    val nextDue = row.nextDue
    val lateSince = row.lateSince
    return when {
        row.state == RowState.LATE && lateSince != null -> {
            resources.getString(R.string.tasks_details_late, lateDayText(lateSince, today, locale))
        }

        row.state == RowState.UPCOMING && nextDue == null -> {
            val done = row.target > 0 && row.done >= row.target
            resources.getString(if (done) R.string.tasks_details_done else R.string.tasks_details_missed)
        }

        row.state == RowState.UPCOMING && nextDue != null && showsNextWeekday(row.task.repeat) -> {
            scheduleText(resources, row.task) + SEPARATOR + nextDue.dayOfWeek.getDisplayName(TextStyle.SHORT, locale)
        }

        else -> {
            scheduleText(resources, row.task)
        }
    }
}

private fun showsNextWeekday(repeat: Repeat): Boolean = repeat is Repeat.EveryNDays || repeat is Repeat.Monthly

private fun lateDayText(
    day: LocalDate,
    today: LocalDate,
    locale: Locale,
): String =
    if (day.plusDays(DAYS_IN_WEEK.toLong()).isAfter(today)) {
        day.dayOfWeek.getDisplayName(TextStyle.SHORT, locale)
    } else {
        day.format(DateTimeFormatter.ofPattern("d MMM", locale))
    }

private fun repeatText(
    resources: Resources,
    repeat: Repeat,
): String? =
    when (repeat) {
        is Repeat.Weekdays -> {
            weekdaysText(resources, repeat.days)
        }

        is Repeat.EveryNDays -> {
            resources.getQuantityString(R.plurals.tasks_schedule_every_n_days, repeat.days, repeat.days)
        }

        is Repeat.Monthly -> {
            resources.getString(
                R.string.tasks_schedule_monthly,
                repeat.dayOfMonth,
                ordinalSuffix(resources, repeat.dayOfMonth),
            )
        }

        is Repeat.OneTime -> {
            repeat.due.format(DateTimeFormatter.ofPattern("d MMM", resources.locale()))
        }

        Repeat.EveryDay, is Repeat.TimesPerDay -> {
            null
        }
    }

private fun weekdaysText(
    resources: Resources,
    days: Set<DayOfWeek>,
): String =
    when (days) {
        workWeek -> resources.getString(R.string.tasks_schedule_every_weekday)
        DayOfWeek.entries.toSet() -> resources.getString(R.string.tasks_schedule_every_day)
        else -> days.sorted().joinToString(", ") { it.getDisplayName(TextStyle.SHORT, resources.locale()) }
    }

/** English needs "1st, 2nd, 3rd, 11th"; other languages map all four suffixes to the same text. */
private fun ordinalSuffix(
    resources: Resources,
    day: Int,
): String {
    val teen = day % TEENS_CYCLE in FIRST_TEEN..LAST_TEEN
    val suffix =
        when {
            teen -> R.string.tasks_ordinal_th
            day % TENS_CYCLE == 1 -> R.string.tasks_ordinal_st
            day % TENS_CYCLE == 2 -> R.string.tasks_ordinal_nd
            day % TENS_CYCLE == 3 -> R.string.tasks_ordinal_rd
            else -> R.string.tasks_ordinal_th
        }
    return resources.getString(suffix)
}
