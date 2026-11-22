package io.github.ddiling.mydayboard.feature.tasks.form

import io.github.ddiling.mydayboard.core.domain.task.EveryNDaysMode
import io.github.ddiling.mydayboard.core.domain.task.MAX_INTERVAL_DAYS
import io.github.ddiling.mydayboard.core.domain.task.MIN_INTERVAL_DAYS
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

/** Keeps the times sorted and distinct, as a stored task has them, so the chips never show a repeat or jump. */
internal fun TaskFormUiState.withTimeAdded(time: LocalTime) =
    copy(times = (times + time).distinct().sorted()).revalidated()

internal fun TaskFormUiState.withTimeRemoved(time: LocalTime) = copy(times = times - time).revalidated()

internal fun TaskFormUiState.withWeekdayToggled(day: DayOfWeek) =
    copy(weekdays = if (day in weekdays) weekdays - day else weekdays + day).revalidated()

/** The stepper cannot leave its range, so an out-of-range interval never reaches validation. */
internal fun TaskFormUiState.withInterval(days: Int) =
    copy(intervalDays = days.coerceIn(MIN_INTERVAL_DAYS, MAX_INTERVAL_DAYS)).revalidated()

/**
 * Counting from the last time has no "Starting on" field, so a date picked for the fixed calendar must not stay
 * hidden in the state: it would show in the dates line and be saved. Switching to it starts the task today.
 */
internal fun TaskFormUiState.withIntervalMode(mode: EveryNDaysMode) =
    copy(
        intervalMode = mode,
        firstDue = if (mode == EveryNDaysMode.FROM_LAST_DONE) today else firstDue,
    ).revalidated()

internal fun TaskFormUiState.withFirstDue(date: LocalDate) = copy(firstDue = date).revalidated()

/** Stored as typed, so 0 or 32 shows the range error instead of being silently moved into the range. */
internal fun TaskFormUiState.withDayOfMonth(day: Int) = copy(dayOfMonth = day).revalidated()

internal fun TaskFormUiState.withDueDate(date: LocalDate) = copy(dueDate = date).revalidated()

internal fun TaskFormUiState.withTime(time: LocalTime?) = copy(time = time).revalidated()
