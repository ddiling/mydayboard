package io.github.ddiling.mydayboard.feature.tasks.form

import io.github.ddiling.mydayboard.core.domain.task.MIN_TASK_GOAL
import io.github.ddiling.mydayboard.core.domain.task.OnMiss
import io.github.ddiling.mydayboard.core.domain.task.Repeat
import io.github.ddiling.mydayboard.core.domain.task.Task
import io.github.ddiling.mydayboard.core.domain.task.TaskDraft
import io.github.ddiling.mydayboard.core.domain.task.validateTaskDraft
import java.time.LocalDate

/** Daily types start a new occurrence at midnight, so "stays until done" would change nothing and is hidden. */
internal fun RepeatType.hidesStays(): Boolean = this == RepeatType.TIMES_PER_DAY || this == RepeatType.EVERY_DAY

/** A tick per time already is the count of a several-times-a-day task, so it has no count goal. */
internal fun RepeatType.hidesGoal(): Boolean = this == RepeatType.TIMES_PER_DAY

/** The missed-task setting a repeat type starts with: a bill stays until paid, a chore expires. */
internal fun RepeatType.defaultOnMiss(): OnMiss = if (this == RepeatType.ONE_TIME) OnMiss.STAYS else OnMiss.EXPIRES

private fun RepeatType.hasTime(): Boolean = this != RepeatType.TIMES_PER_DAY && this != RepeatType.EVERY_N_DAYS

/**
 * The draft the form would save. Hidden settings are forced to their value here too, so a stale field can never
 * reach storage, and a several-times-a-day task never has a time of its own (its times live in the repeat).
 * A one-time task without a due date falls back to [TaskFormUiState.firstDue]; `canSave` blocks that case.
 */
internal fun TaskFormUiState.toDraft(): TaskDraft =
    TaskDraft(
        name = name,
        categoryId = categoryId,
        repeat = toRepeat(),
        time = time.takeIf { repeatType.hasTime() },
        note = note.trim().takeIf { it.isNotEmpty() },
        goal = goal.takeIf { goalOn && !repeatType.hidesGoal() },
        onMiss = if (repeatType.hidesStays()) OnMiss.EXPIRES else onMiss,
    )

private fun TaskFormUiState.toRepeat(): Repeat =
    when (repeatType) {
        RepeatType.TIMES_PER_DAY -> Repeat.TimesPerDay(times)
        RepeatType.EVERY_DAY -> Repeat.EveryDay
        RepeatType.WEEKDAYS -> Repeat.Weekdays(weekdays)
        RepeatType.EVERY_N_DAYS -> Repeat.EveryNDays(intervalDays, intervalMode, firstDue)
        RepeatType.MONTHLY -> Repeat.Monthly(dayOfMonth)
        RepeatType.ONE_TIME -> Repeat.OneTime(dueDate ?: firstDue)
    }

/** Recomputes [TaskFormUiState.errors] and `canSave`; every reducer ends with it. */
internal fun TaskFormUiState.revalidated(): TaskFormUiState {
    val found = validateTaskDraft(toDraft())
    val hasDueDate = repeatType != RepeatType.ONE_TIME || dueDate != null
    return copy(errors = found, canSave = found.isEmpty() && hasDueDate && load == FormLoad.Ready && !saved)
}

/**
 * The day a new task counts from: today. A one-time task whose due date is already past counts from that date
 * instead, so a bill due last week still shows up in its stats.
 */
internal fun TaskFormUiState.startDate(today: LocalDate): LocalDate =
    if (repeatType == RepeatType.ONE_TIME && dueDate != null) minOf(today, dueDate) else today

/** Loads a stored task into the form. The task's start date is not editable, so it is not part of the form. */
internal fun Task.toFormState(today: LocalDate): TaskFormUiState {
    val base =
        TaskFormUiState(
            today = today,
            name = name,
            categoryId = categoryId,
            time = time,
            note = note.orEmpty(),
            goalOn = goal != null,
            goal = goal ?: MIN_TASK_GOAL,
            onMiss = onMiss,
            isEditing = true,
        )
    return base.withStoredRepeat(repeat).revalidated()
}

private fun TaskFormUiState.withStoredRepeat(repeat: Repeat): TaskFormUiState =
    when (repeat) {
        is Repeat.TimesPerDay -> {
            copy(repeatType = RepeatType.TIMES_PER_DAY, times = repeat.times)
        }

        Repeat.EveryDay -> {
            copy(repeatType = RepeatType.EVERY_DAY)
        }

        is Repeat.Weekdays -> {
            copy(repeatType = RepeatType.WEEKDAYS, weekdays = repeat.days)
        }

        is Repeat.EveryNDays -> {
            copy(
                repeatType = RepeatType.EVERY_N_DAYS,
                intervalDays = repeat.days,
                intervalMode = repeat.mode,
                firstDue = repeat.firstDue,
            )
        }

        is Repeat.Monthly -> {
            copy(repeatType = RepeatType.MONTHLY, dayOfMonth = repeat.dayOfMonth)
        }

        is Repeat.OneTime -> {
            copy(repeatType = RepeatType.ONE_TIME, dueDate = repeat.due)
        }
    }
