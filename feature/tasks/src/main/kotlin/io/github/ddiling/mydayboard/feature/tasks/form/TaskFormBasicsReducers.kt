package io.github.ddiling.mydayboard.feature.tasks.form

import io.github.ddiling.mydayboard.core.domain.task.EveryNDaysMode
import io.github.ddiling.mydayboard.core.domain.task.OnMiss

internal fun TaskFormUiState.withName(name: String) = copy(name = name).revalidated()

internal fun TaskFormUiState.withCategory(categoryId: String?) = copy(categoryId = categoryId).revalidated()

internal fun TaskFormUiState.withNote(note: String) = copy(note = note).revalidated()

/** Hidden for a type with no count goal: the switch cannot be turned on there. */
internal fun TaskFormUiState.withGoalOn(on: Boolean) = copy(goalOn = on && !repeatType.hidesGoal()).revalidated()

/** Stored as typed, so a goal of 1 shows its error instead of being silently raised. */
internal fun TaskFormUiState.withGoal(goal: Int) = copy(goal = goal).revalidated()

/** Ignored for a type that hides the setting: those tasks always expire. */
internal fun TaskFormUiState.withOnMiss(onMiss: OnMiss) =
    if (repeatType.hidesStays()) this else copy(onMiss = onMiss).revalidated()

/**
 * Switches the repeat type. Name, category, note and goal are about the task, not its schedule, so they stay;
 * the fields of the schedule start again from their defaults. Hidden settings are forced to their value, and the
 * missed-task default follows a change between one-time and repeating, where the sensible choice differs.
 */
internal fun TaskFormUiState.withRepeatType(type: RepeatType): TaskFormUiState {
    if (type == repeatType) return this
    val crossesOneTime = (type == RepeatType.ONE_TIME) != (repeatType == RepeatType.ONE_TIME)
    return copy(
        repeatType = type,
        times = emptyList(),
        weekdays = emptySet(),
        intervalDays = DEFAULT_INTERVAL_DAYS,
        intervalMode = EveryNDaysMode.FROM_LAST_DONE,
        firstDue = today,
        dayOfMonth = today.dayOfMonth,
        dueDate = null,
        time = time.takeIf { type != RepeatType.TIMES_PER_DAY },
        goalOn = goalOn && !type.hidesGoal(),
        onMiss =
            when {
                type.hidesStays() -> OnMiss.EXPIRES
                crossesOneTime -> type.defaultOnMiss()
                else -> onMiss
            },
    ).revalidated()
}
