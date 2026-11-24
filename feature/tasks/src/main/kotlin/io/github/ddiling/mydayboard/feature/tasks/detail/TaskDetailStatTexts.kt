package io.github.ddiling.mydayboard.feature.tasks.detail

import android.content.res.Resources
import io.github.ddiling.mydayboard.feature.tasks.R
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private const val RECENT_FORMAT = "%.1f"

/** One number of the stats grid and what it counts. */
internal data class StatCell(
    val value: String,
    val label: String,
)

internal fun Resources.deviceLocale(): Locale = configuration.locales[0]

/**
 * The cells of the stats grid. A one-time task has facts only: when it started and how late it is. "Per day"
 * counts ticks for a count goal and credit otherwise; half days appear only for several-times tasks.
 */
internal fun statCells(
    resources: Resources,
    stats: DetailStats,
): List<StatCell> {
    val since = StatCell(dateText(resources, stats.since), resources.getString(R.string.tasks_detail_stat_since))
    return when (stats) {
        is DetailStats.Facts -> listOfNotNull(since, lateCell(resources, stats.daysLate))
        is DetailStats.Recurring -> listOf(since) + recurringCells(resources, stats)
    }
}

private fun lateCell(
    resources: Resources,
    daysLate: Int?,
): StatCell? = daysLate?.let { StatCell(daysText(resources, it), resources.getString(R.string.tasks_detail_stat_late)) }

private fun recurringCells(
    resources: Resources,
    stats: DetailStats.Recurring,
): List<StatCell> {
    val credit = creditText(resources, stats.doneOfPlanned)
    val perDay = String.format(resources.deviceLocale(), RECENT_FORMAT, stats.perDay)
    return listOfNotNull(
        StatCell(daysText(resources, stats.streak), resources.getString(R.string.tasks_detail_stat_streak)),
        StatCell(
            resources.getString(R.string.tasks_detail_stat_of, credit, stats.plannedDays),
            resources.getString(R.string.tasks_detail_stat_recent),
        ),
        StatCell(perDay, resources.getString(R.string.tasks_detail_stat_per_day)),
        StatCell(stats.skipped.toString(), resources.getString(R.string.tasks_detail_stat_skipped)),
        stats.halfDays?.let { StatCell(it.toString(), resources.getString(R.string.tasks_detail_stat_half_days)) },
    )
}

private fun daysText(
    resources: Resources,
    days: Int,
): String = resources.getQuantityString(R.plurals.tasks_detail_days, days, days)

internal fun dateText(
    resources: Resources,
    day: LocalDate,
): String = day.format(DateTimeFormatter.ofPattern("d MMM", resources.deviceLocale()))

/** A half day counts 0.5, so a whole number prints without a decimal ("8") and a half with one ("7.5"). */
private fun creditText(
    resources: Resources,
    credit: Double,
): String =
    if (credit % 1.0 == 0.0) {
        credit.toInt().toString()
    } else {
        String.format(resources.deviceLocale(), RECENT_FORMAT, credit)
    }
