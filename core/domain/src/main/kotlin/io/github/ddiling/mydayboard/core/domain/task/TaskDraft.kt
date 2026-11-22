package io.github.ddiling.mydayboard.core.domain.task

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

/** What happens to an occurrence the user did not complete in its window. */
enum class OnMiss {
    /** The occurrence stays open (late) until the next due date, so it can still be done. */
    STAYS,

    /** The occurrence ends on its due day and counts as skipped afterwards. */
    EXPIRES,
}

/** How the due dates of an [Repeat.EveryNDays] task are laid out. */
enum class EveryNDaysMode {
    /** The next due date is counted from the day the last occurrence was completed. */
    FROM_LAST_DONE,

    /** Due dates are fixed on the calendar, `firstDue + k * days`, whatever the user did. */
    FIXED_CALENDAR,
}

/**
 * When a task is due. A sealed type rather than nullable fields on [Task], because each kind needs different
 * data and a flat shape would allow impossible combinations, such as a weekday set on a monthly task.
 */
sealed interface Repeat {
    /** Several times a day, each at its own time. [times] is sorted, distinct and has at least one entry. */
    data class TimesPerDay(
        val times: List<LocalTime>,
    ) : Repeat

    /** Once every day. */
    data object EveryDay : Repeat

    /** On the chosen [days] of the week; at least one. */
    data class Weekdays(
        val days: Set<DayOfWeek>,
    ) : Repeat

    /** Every [days] days, starting at [firstDue], laid out according to [mode]. */
    data class EveryNDays(
        val days: Int,
        val mode: EveryNDaysMode,
        val firstDue: LocalDate,
    ) : Repeat

    /** On [dayOfMonth] each month; in shorter months the last day of the month. */
    data class Monthly(
        val dayOfMonth: Int,
    ) : Repeat

    /** Once, on [due]. */
    data class OneTime(
        val due: LocalDate,
    ) : Repeat
}

/** What the task form produces: everything the user chooses, before the repository adds identity and dates. */
data class TaskDraft(
    val name: String,
    val categoryId: String?,
    val repeat: Repeat,
    /** Optional time of day; null for [Repeat.TimesPerDay], whose times live in the repeat. */
    val time: LocalTime?,
    val note: String?,
    /** How many ticks complete one occurrence; null means one. */
    val goal: Int?,
    val onMiss: OnMiss,
)
