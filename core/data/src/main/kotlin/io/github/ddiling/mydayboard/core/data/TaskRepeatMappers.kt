package io.github.ddiling.mydayboard.core.data

import io.github.ddiling.mydayboard.core.database.model.TaskEntity
import io.github.ddiling.mydayboard.core.database.model.TaskEntityIntervalMode
import io.github.ddiling.mydayboard.core.database.model.TaskEntityRepeatType
import io.github.ddiling.mydayboard.core.domain.task.EveryNDaysMode
import io.github.ddiling.mydayboard.core.domain.task.Repeat
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

private val timeFormat = DateTimeFormatter.ofPattern("HH:mm")
private const val TIMES_SEPARATOR = ","

/** Bit 0 is Monday and bit 6 is Sunday, the order of [DayOfWeek], so a saved mask never changes meaning. */
internal fun Set<DayOfWeek>.toMask(): Int = fold(0) { mask, day -> mask or (1 shl day.ordinal) }

internal fun Int.toWeekdays(): Set<DayOfWeek> = DayOfWeek.entries.filter { this and (1 shl it.ordinal) != 0 }.toSet()

internal fun LocalTime.toText(): String = format(timeFormat)

internal fun String.toTime(): LocalTime = LocalTime.parse(this, timeFormat)

/** A row with a missing column for its repeat type is corrupt data, which must fail loudly, not be guessed. */
private fun <T> TaskEntity.required(
    value: T?,
    column: String,
): T = checkNotNull(value) { "Task $id of type $repeatType has no $column" }

internal fun TaskEntity.toRepeat(): Repeat =
    when (repeatType) {
        TaskEntityRepeatType.TIMES_PER_DAY -> {
            Repeat.TimesPerDay(required(times, "times").split(TIMES_SEPARATOR).map { it.toTime() })
        }

        TaskEntityRepeatType.EVERY_DAY -> {
            Repeat.EveryDay
        }

        TaskEntityRepeatType.WEEKDAYS -> {
            Repeat.Weekdays(required(weekdays, "weekdays").toWeekdays())
        }

        TaskEntityRepeatType.EVERY_N_DAYS -> {
            Repeat.EveryNDays(
                days = required(intervalDays, "interval_days"),
                mode = EveryNDaysMode.valueOf(required(intervalMode, "interval_mode").name),
                firstDue = LocalDate.ofEpochDay(required(anchorDate, "anchor_date")),
            )
        }

        TaskEntityRepeatType.MONTHLY -> {
            Repeat.Monthly(required(dayOfMonth, "day_of_month"))
        }

        TaskEntityRepeatType.ONE_TIME -> {
            Repeat.OneTime(LocalDate.ofEpochDay(required(anchorDate, "anchor_date")))
        }
    }

internal fun Repeat.toEntityType(): TaskEntityRepeatType =
    when (this) {
        is Repeat.TimesPerDay -> TaskEntityRepeatType.TIMES_PER_DAY
        Repeat.EveryDay -> TaskEntityRepeatType.EVERY_DAY
        is Repeat.Weekdays -> TaskEntityRepeatType.WEEKDAYS
        is Repeat.EveryNDays -> TaskEntityRepeatType.EVERY_N_DAYS
        is Repeat.Monthly -> TaskEntityRepeatType.MONTHLY
        is Repeat.OneTime -> TaskEntityRepeatType.ONE_TIME
    }

/** Fills only the columns of [repeat]'s own type; the others stay null. */
internal fun TaskEntity.withRepeatColumns(repeat: Repeat): TaskEntity {
    val base = this
    return when (val r = repeat) {
        is Repeat.TimesPerDay -> {
            base.copy(times = r.times.joinToString(TIMES_SEPARATOR) { it.toText() })
        }

        Repeat.EveryDay -> {
            base
        }

        is Repeat.Weekdays -> {
            base.copy(weekdays = r.days.toMask())
        }

        is Repeat.EveryNDays -> {
            base.copy(
                intervalDays = r.days,
                intervalMode = TaskEntityIntervalMode.valueOf(r.mode.name),
                anchorDate = r.firstDue.toEpochDay(),
            )
        }

        is Repeat.Monthly -> {
            base.copy(dayOfMonth = r.dayOfMonth)
        }

        is Repeat.OneTime -> {
            base.copy(anchorDate = r.due.toEpochDay())
        }
    }
}
