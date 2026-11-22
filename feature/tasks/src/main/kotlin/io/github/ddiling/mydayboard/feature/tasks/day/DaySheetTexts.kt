package io.github.ddiling.mydayboard.feature.tasks.day

import android.content.res.Resources
import io.github.ddiling.mydayboard.feature.tasks.R
import io.github.ddiling.mydayboard.feature.tasks.detail.deviceLocale
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** "Sat 21.11", the day as the sheet and its message name it. */
internal fun daySheetDayText(
    resources: Resources,
    day: LocalDate,
): String = day.format(DateTimeFormatter.ofPattern("EEE d.MM", resources.deviceLocale()))

/** "Lunch · Sat 21.11": the sheet's title and the start of its message. */
internal fun daySheetTitle(
    resources: Resources,
    state: DaySheetState,
): String = resources.getString(R.string.tasks_day_title, state.taskName, daySheetDayText(resources, state.day))

/** The message after Save: what was marked for a yes/no day, and a plain "updated" for times and counts. */
internal fun dayMarkedText(
    resources: Resources,
    marked: DayMarked,
): String {
    val name = marked.state.taskName
    val day = daySheetDayText(resources, marked.state.day)
    val kind = marked.state.kind
    val format =
        when {
            kind is DaySheetKind.Single && kind.done -> R.string.tasks_day_marked_done
            kind is DaySheetKind.Single -> R.string.tasks_day_marked_not_done
            else -> R.string.tasks_day_updated
        }
    return resources.getString(format, name, day)
}
