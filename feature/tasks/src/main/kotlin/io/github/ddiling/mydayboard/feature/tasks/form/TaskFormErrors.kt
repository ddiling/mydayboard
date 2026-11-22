package io.github.ddiling.mydayboard.feature.tasks.form

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.github.ddiling.mydayboard.core.domain.task.TaskFormError
import io.github.ddiling.mydayboard.feature.tasks.R

/** The text for the errors that have a field to sit under. */
@Composable
internal fun TaskFormError?.message(): String? =
    when (this) {
        TaskFormError.BlankName -> stringResource(R.string.tasks_form_error_name_blank)
        is TaskFormError.NameTooLong -> stringResource(R.string.tasks_form_error_name_too_long, max)
        is TaskFormError.NoteTooLong -> stringResource(R.string.tasks_form_error_note_too_long, max)
        TaskFormError.NoTimes -> stringResource(R.string.tasks_form_error_no_times)
        TaskFormError.NoWeekdays -> stringResource(R.string.tasks_form_error_no_days)
        TaskFormError.DayOfMonthOutOfRange -> stringResource(R.string.tasks_form_error_day_of_month)
        is TaskFormError.GoalOutOfRange -> stringResource(R.string.tasks_form_error_goal, min, max)
        else -> null
    }
