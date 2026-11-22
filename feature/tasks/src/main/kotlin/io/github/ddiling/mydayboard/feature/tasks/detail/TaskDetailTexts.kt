package io.github.ddiling.mydayboard.feature.tasks.detail

import android.content.res.Resources
import io.github.ddiling.mydayboard.core.domain.task.Task
import io.github.ddiling.mydayboard.feature.tasks.R
import io.github.ddiling.mydayboard.feature.tasks.common.scheduleText
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle

private val timeFormat = DateTimeFormatter.ofPattern("HH:mm")
private const val SEPARATOR = " · "
private const val DAYS_IN_WEEK = 7L

/** The big status word. A late day within the last week is a weekday, older ones a date, as in the list. */
internal fun heroText(
    resources: Resources,
    hero: Hero,
    today: LocalDate,
): String =
    when (hero) {
        Hero.NotDone -> {
            resources.getString(R.string.tasks_detail_hero_not_done)
        }

        Hero.DoneToday -> {
            resources.getString(R.string.tasks_detail_hero_done_today)
        }

        Hero.NotDueToday -> {
            resources.getString(R.string.tasks_detail_hero_not_due)
        }

        is Hero.TimesProgress -> {
            resources.getQuantityString(R.plurals.tasks_detail_hero_times, hero.target, hero.done, hero.target)
        }

        is Hero.CountProgress -> {
            resources.getQuantityString(R.plurals.tasks_detail_hero_count, hero.target, hero.done, hero.target)
        }

        is Hero.LateSince -> {
            resources.getString(R.string.tasks_detail_hero_late, recentDayText(resources, hero.day, today))
        }

        is Hero.Finished -> {
            resources.getString(if (hero.reached) R.string.tasks_details_done else R.string.tasks_details_missed)
        }
    }

/** The grey line under the status word: how it repeats, its note, its category. Empty parts are left out. */
internal fun subtitleText(
    resources: Resources,
    task: Task,
    categoryName: String?,
): String = listOfNotNull(scheduleText(resources, task), task.note, categoryName).joinToString(SEPARATOR)

/** "Next: Sat 28.11", for a task that is not due today. */
internal fun nextDueText(
    resources: Resources,
    day: LocalDate,
): String {
    val format = DateTimeFormatter.ofPattern("EEE d.MM", resources.deviceLocale())
    return resources.getString(R.string.tasks_detail_next, day.format(format))
}

internal fun bigButtonText(
    resources: Resources,
    button: BigButton,
): String? =
    when (button) {
        BigButton.MarkDone -> resources.getString(R.string.tasks_detail_button_done)
        is BigButton.TickNext -> resources.getString(R.string.tasks_detail_button_tick, button.time.format(timeFormat))
        BigButton.PlusOne -> resources.getString(R.string.tasks_detail_button_plus_one)
        BigButton.UntickToday -> resources.getString(R.string.tasks_detail_button_untick)
        BigButton.None -> null
    }

internal fun historyResultText(
    resources: Resources,
    result: HistoryResult,
): String =
    when (result) {
        HistoryResult.Done -> resources.getString(R.string.tasks_details_done)
        HistoryResult.Missed -> resources.getString(R.string.tasks_details_missed)
        HistoryResult.Late -> resources.getString(R.string.tasks_details_late_open)
        is HistoryResult.Partial -> partialText(resources, result)
    }

private fun partialText(
    resources: Resources,
    partial: HistoryResult.Partial,
): String {
    val id =
        when {
            partial.severalTimes && partial.settled -> R.string.tasks_detail_history_half_day
            partial.severalTimes -> R.string.tasks_detail_history_times
            partial.settled -> R.string.tasks_detail_history_not_reached
            else -> R.string.tasks_detail_history_count
        }
    return resources.getString(id, partial.done, partial.target)
}

/** "Today", "Yesterday", or the weekday and date of an older day. */
internal fun historyDayText(
    resources: Resources,
    day: LocalDate,
    today: LocalDate,
): String =
    when (day) {
        today -> resources.getString(R.string.tasks_detail_day_today)
        today.minusDays(1) -> resources.getString(R.string.tasks_detail_day_yesterday)
        else -> day.format(DateTimeFormatter.ofPattern("EEE d MMM", resources.deviceLocale()))
    }

private fun recentDayText(
    resources: Resources,
    day: LocalDate,
    today: LocalDate,
): String =
    if (day.plusDays(DAYS_IN_WEEK).isAfter(today)) {
        day.dayOfWeek.getDisplayName(TextStyle.SHORT, resources.deviceLocale())
    } else {
        dateText(resources, day)
    }
