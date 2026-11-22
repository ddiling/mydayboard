package io.github.ddiling.mydayboard.feature.tasks.form

import io.github.ddiling.mydayboard.core.domain.task.EveryNDaysMode
import io.github.ddiling.mydayboard.core.domain.task.MIN_TASK_GOAL
import io.github.ddiling.mydayboard.core.domain.task.OnMiss
import io.github.ddiling.mydayboard.core.domain.task.TaskFormError
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

/** The six ways to repeat, as the form's dropdown lists them. [Repeat] itself only exists once the fields are valid. */
internal enum class RepeatType { TIMES_PER_DAY, EVERY_DAY, WEEKDAYS, EVERY_N_DAYS, MONTHLY, ONE_TIME }

/** Whether the task being edited has been looked up yet, and whether it exists. */
internal enum class FormLoad { Loading, Ready, NotFound }

/** The interval a fresh "every N days" task starts with. */
internal const val DEFAULT_INTERVAL_DAYS = 3

/**
 * Everything the task form shows, as one immutable object, so a change to one field cannot leave another stale.
 * Fields of other repeat types keep their last value only until the type changes (then they are reset), so the
 * form never offers a stale weekday set under "Once a month". [errors] is always current; the screen decides when
 * to show them, so an untouched empty name does not scold the user at once. [saved] is set when a save has
 * finished and navigation should leave the form; the screen clears it with `onSavedHandled`.
 */
internal data class TaskFormUiState(
    val today: LocalDate,
    val load: FormLoad = FormLoad.Ready,
    val name: String = "",
    val categoryId: String? = null,
    val repeatType: RepeatType = RepeatType.EVERY_DAY,
    val times: List<LocalTime> = emptyList(),
    val weekdays: Set<DayOfWeek> = emptySet(),
    val intervalDays: Int = DEFAULT_INTERVAL_DAYS,
    val intervalMode: EveryNDaysMode = EveryNDaysMode.FROM_LAST_DONE,
    val firstDue: LocalDate = today,
    val dayOfMonth: Int = today.dayOfMonth,
    val dueDate: LocalDate? = null,
    val time: LocalTime? = null,
    val note: String = "",
    val goalOn: Boolean = false,
    val goal: Int = MIN_TASK_GOAL,
    val onMiss: OnMiss = OnMiss.EXPIRES,
    val errors: List<TaskFormError> = emptyList(),
    val isEditing: Boolean = false,
    val canSave: Boolean = false,
    val saved: Boolean = false,
)
