package io.github.ddiling.mydayboard.feature.tasks.form

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import io.github.ddiling.mydayboard.feature.tasks.R
import java.time.LocalTime

/**
 * The Material 3 clock wrapped once, so every time field of the form opens the same dialog. The clock scrolls because
 * its dial has a fixed size: at a large font or in landscape it would otherwise push OK and Cancel out of view.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TimePickerDialog(
    initial: LocalTime,
    onConfirm: (LocalTime) -> Unit,
    onDismiss: () -> Unit,
) {
    val pickerState = rememberTimePickerState(initial.hour, initial.minute, is24Hour = true)
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onConfirm(LocalTime.of(pickerState.hour, pickerState.minute)) }) {
                Text(stringResource(R.string.tasks_form_ok))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.tasks_form_cancel)) } },
        text = { TimePicker(pickerState, Modifier.verticalScroll(rememberScrollState())) },
    )
}
