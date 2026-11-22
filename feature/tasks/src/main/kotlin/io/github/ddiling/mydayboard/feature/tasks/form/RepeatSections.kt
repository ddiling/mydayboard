package io.github.ddiling.mydayboard.feature.tasks.form

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.ddiling.mydayboard.core.domain.task.TaskFormError
import io.github.ddiling.mydayboard.feature.tasks.R
import java.time.LocalTime
import java.time.format.DateTimeFormatter

private val timeFormat = DateTimeFormatter.ofPattern("HH:mm")

/** The time shown on chips and in fields: 24-hour, so the picker (also 24-hour) and the text always agree. */
internal fun LocalTime.display(): String = format(timeFormat)

/** The fields that depend on the repeat type; the note and the time show only where they make sense. */
@Composable
internal fun RepeatSections(
    uiState: TaskFormUiState,
    actions: TaskFormActions,
) {
    val noteError = uiState.errors.firstOrNull { it is TaskFormError.NoteTooLong }
    val note = @Composable {
        DashedTextField(
            label = stringResource(R.string.tasks_form_note),
            value = uiState.note,
            onValueChange = actions.onNoteChange,
            error = noteError.message(),
            modifier = Modifier.testTag("tasks_form_note"),
        )
    }
    when (uiState.repeatType) {
        RepeatType.TIMES_PER_DAY -> {
            TimesSection(uiState.times, TaskFormError.NoTimes in uiState.errors, actions)
            note()
        }

        RepeatType.EVERY_DAY -> {
            TimeField(uiState.time, actions.onTimeChange)
            note()
        }

        RepeatType.WEEKDAYS -> {
            WeekdaysSection(uiState, actions)
            TimeField(uiState.time, actions.onTimeChange)
        }

        RepeatType.EVERY_N_DAYS -> {
            IntervalSection(uiState, actions)
        }

        RepeatType.MONTHLY -> {
            DashedNumberField(
                label = stringResource(R.string.tasks_form_on_day),
                value = uiState.dayOfMonth,
                onValueChange = actions.onDayOfMonthChange,
                error = (TaskFormError.DayOfMonthOutOfRange.takeIf { it in uiState.errors }).message(),
                modifier = Modifier.testTag("tasks_form_day_of_month"),
            )
            Text(
                stringResource(R.string.tasks_form_on_day_help),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TimeField(uiState.time, actions.onTimeChange)
        }

        RepeatType.ONE_TIME -> {
            DateField(
                label = stringResource(R.string.tasks_form_due_date),
                date = uiState.dueDate,
                opensOn = uiState.today,
                onDateChange = actions.onDueDateChange,
                tag = "tasks_form_due_date",
                error = stringResource(R.string.tasks_form_error_due_required).takeIf { uiState.dueDate == null },
            )
            TimeField(uiState.time, actions.onTimeChange)
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
internal fun SectionError(
    error: TaskFormError,
    shown: Boolean,
) {
    if (shown) {
        Text(
            error.message().orEmpty(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TimesSection(
    times: List<LocalTime>,
    showError: Boolean,
    actions: TaskFormActions,
) {
    var picking by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionTitle(stringResource(R.string.tasks_form_times_title))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            times.forEach { time ->
                InputChip(
                    selected = false,
                    onClick = { actions.onRemoveTime(time) },
                    label = { Text(time.display()) },
                    trailingIcon = {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = stringResource(R.string.tasks_form_remove_time, time.display()),
                        )
                    },
                    border = null,
                    colors =
                        InputChipDefaults.inputChipColors(
                            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = CHIP_ALPHA),
                        ),
                )
            }
            AssistChip(
                onClick = { picking = true },
                label = { Text(stringResource(R.string.tasks_form_add_time)) },
                leadingIcon = { Icon(Icons.Default.Add, contentDescription = null) },
                border = null,
                colors =
                    AssistChipDefaults.assistChipColors(
                        labelColor = MaterialTheme.colorScheme.primary,
                        leadingIconContentColor = MaterialTheme.colorScheme.primary,
                    ),
            )
        }
        SectionError(TaskFormError.NoTimes, showError)
    }
    if (picking) {
        TimePickerDialog(
            initial = DEFAULT_PICKER_TIME,
            onConfirm = {
                picking = false
                actions.onAddTime(it)
            },
            onDismiss = { picking = false },
        )
    }
}

/** The time a new picker starts on: a morning hour most habits fit. */
internal val DEFAULT_PICKER_TIME: LocalTime = LocalTime.of(9, 0)

private const val CHIP_ALPHA = 0.16f

/** An optional time: tap to pick, the x clears it. */
@Composable
internal fun TimeField(
    time: LocalTime?,
    onTimeChange: (LocalTime?) -> Unit,
) {
    var picking by remember { mutableStateOf(false) }
    DashedField(stringResource(R.string.tasks_form_time_optional)) {
        Row(
            Modifier.fillMaxWidth().clickable { picking = true }.testTag("tasks_form_time"),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(time?.display().orEmpty(), Modifier.weight(1f))
            if (time != null) {
                IconButton(onClick = { onTimeChange(null) }) {
                    Icon(Icons.Default.Close, contentDescription = stringResource(R.string.tasks_form_clear_time))
                }
            }
        }
    }
    if (picking) {
        TimePickerDialog(
            initial = time ?: DEFAULT_PICKER_TIME,
            onConfirm = {
                picking = false
                onTimeChange(it)
            },
            onDismiss = { picking = false },
        )
    }
}
