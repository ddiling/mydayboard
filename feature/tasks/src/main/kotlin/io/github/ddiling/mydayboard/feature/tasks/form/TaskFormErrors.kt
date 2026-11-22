package io.github.ddiling.mydayboard.feature.tasks.form

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.github.ddiling.mydayboard.core.domain.task.TaskFormError
import io.github.ddiling.mydayboard.feature.tasks.R

/** The text for the errors the basic repeat types can produce; the others have no field here yet. */
@Composable
internal fun TaskFormError?.message(): String? =
    when (this) {
        TaskFormError.BlankName -> stringResource(R.string.tasks_form_error_name_blank)
        is TaskFormError.NameTooLong -> stringResource(R.string.tasks_form_error_name_too_long, max)
        else -> null
    }
