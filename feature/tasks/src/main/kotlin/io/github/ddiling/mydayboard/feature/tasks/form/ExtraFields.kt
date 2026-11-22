package io.github.ddiling.mydayboard.feature.tasks.form

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.github.ddiling.mydayboard.core.domain.task.EveryNDaysMode
import io.github.ddiling.mydayboard.core.domain.task.MAX_TASK_GOAL
import io.github.ddiling.mydayboard.core.domain.task.MIN_TASK_GOAL
import io.github.ddiling.mydayboard.core.domain.task.OnMiss
import io.github.ddiling.mydayboard.feature.tasks.R
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@Composable
internal fun NumberField(
    label: String,
    value: Int,
    range: IntRange,
    onChange: (Int) -> Unit,
) {
    var text by remember(value) { mutableStateOf(value.toString()) }
    DashedField(label) {
        androidx.compose.foundation.text.BasicTextField(
            value = text,
            onValueChange = { input ->
                text = input
                input.toIntOrNull()?.takeIf { it in range }?.let(onChange)
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            textStyle =
                androidx.compose.material3.MaterialTheme.typography.bodyLarge
                    .copy(color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface),
            cursorBrush =
                androidx.compose.ui.graphics
                    .SolidColor(androidx.compose.material3.MaterialTheme.colorScheme.primary),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DateField(
    label: String,
    date: LocalDate?,
    onChange: (LocalDate) -> Unit,
) {
    var picking by remember { mutableStateOf(false) }
    DashedField(label) {
        Text(date?.toString().orEmpty(), Modifier.fillMaxWidth().clickable { picking = true })
    }
    if (picking) {
        val state =
            rememberDatePickerState(
                initialSelectedDateMillis = date?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli(),
            )
        DatePickerDialog(
            onDismissRequest = { picking = false },
            confirmButton = {
                TextButton(onClick = {
                    picking = false
                    state.selectedDateMillis?.let {
                        onChange(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate())
                    }
                }) { Text(stringResource(R.string.tasks_form_ok)) }
            },
            dismissButton = {
                TextButton(onClick = { picking = false }) { Text(stringResource(R.string.tasks_form_cancel)) }
            },
        ) { DatePicker(state) }
    }
}

@Composable
internal fun EveryNDaysSection(
    uiState: TaskFormUiState,
    actions: TaskFormActions,
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        NumberField(stringResource(R.string.tasks_form_every_n), uiState.intervalDays, 1..365, actions.onIntervalChange)
        ModeRadio(
            stringResource(R.string.tasks_form_mode_from_last),
            uiState.intervalMode == EveryNDaysMode.FROM_LAST_DONE,
        ) { actions.onIntervalModeChange(EveryNDaysMode.FROM_LAST_DONE) }
        ModeRadio(
            stringResource(R.string.tasks_form_mode_fixed),
            uiState.intervalMode == EveryNDaysMode.FIXED_CALENDAR,
        ) { actions.onIntervalModeChange(EveryNDaysMode.FIXED_CALENDAR) }
        DateField(stringResource(R.string.tasks_form_first_due), uiState.firstDue, actions.onFirstDueChange)
    }
}

@Composable
private fun ModeRadio(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick), verticalAlignment = Alignment.CenterVertically) {
        RadioButton(selected, onClick)
        Text(text)
    }
}

/** Goal switch + number, and the missed setting. */
@Composable
internal fun ExtraSections(
    uiState: TaskFormUiState,
    actions: TaskFormActions,
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.tasks_form_goal_switch), Modifier.weight(1f))
            Switch(uiState.goalOn, actions.onGoalOnChange)
        }
        if (uiState.goalOn) {
            NumberField(stringResource(R.string.tasks_form_goal), uiState.goal, MIN_TASK_GOAL..MAX_TASK_GOAL, actions.onGoalChange)
        }
        Text(stringResource(R.string.tasks_form_on_miss))
        ModeRadio(stringResource(R.string.tasks_form_on_miss_stays), uiState.onMiss == OnMiss.STAYS) {
            actions.onOnMissChange(OnMiss.STAYS)
        }
        ModeRadio(stringResource(R.string.tasks_form_on_miss_expires), uiState.onMiss == OnMiss.EXPIRES) {
            actions.onOnMissChange(OnMiss.EXPIRES)
        }
    }
}

@Composable
internal fun TextEntryDialog(
    title: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { DashedTextField(title, text, { text = it }) },
        confirmButton = { TextButton(onClick = { onConfirm(text) }) { Text(stringResource(R.string.tasks_form_ok)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.tasks_form_cancel)) } },
    )
}
