package io.github.ddiling.mydayboard.feature.trackers.event

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.ddiling.mydayboard.feature.trackers.R
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private enum class OpenPicker { NONE, DATE, TIME }

private val MinTouchTarget = 48.dp

/**
 * Shows the picked date and time of an entry. The pickers open on top, from the two buttons. The error is
 * written out as text, never only coloured, and Save stays disabled while there is one. Delete is offered
 * only when editing, since an entry being added has nothing to delete.
 */
@Composable
internal fun EventTimeDialog(
    state: EventTimeEditorState,
    is24Hour: Boolean,
    actions: EventTimeDialogActions,
) {
    var openPicker by remember { mutableStateOf(OpenPicker.NONE) }
    val adding = state.mode is EntryMode.Add
    val title = if (adding) R.string.trackers_event_add_title else R.string.trackers_event_edit_title
    AlertDialog(
        onDismissRequest = actions.onDismiss,
        title = { Text(stringResource(title)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                val date = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).format(state.date)
                val time = DateTimeFormatter.ofPattern(if (is24Hour) "HH:mm" else "h:mm a").format(state.time)
                FieldButton(stringResource(R.string.trackers_event_date, date), "trackers_event_date") {
                    openPicker = OpenPicker.DATE
                }
                FieldButton(stringResource(R.string.trackers_event_time, time), "trackers_event_time") {
                    openPicker = OpenPicker.TIME
                }
                state.error?.let {
                    Text(
                        stringResource(it.messageRes()),
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.testTag("trackers_event_error"),
                    )
                }
                if (state.mode is EntryMode.Edit) {
                    TextButton(
                        onClick = actions.onDelete,
                        modifier = Modifier.heightIn(min = MinTouchTarget).testTag("trackers_event_delete"),
                    ) { Text(stringResource(R.string.trackers_event_delete)) }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = actions.onSave,
                enabled = state.canSave,
                modifier = Modifier.heightIn(min = MinTouchTarget).testTag("trackers_event_save"),
            ) { Text(stringResource(R.string.trackers_editor_save)) }
        },
        dismissButton = {
            TextButton(onClick = actions.onDismiss, modifier = Modifier.heightIn(min = MinTouchTarget)) {
                Text(stringResource(R.string.trackers_editor_cancel))
            }
        },
    )
    val closePicker = { openPicker = OpenPicker.NONE }
    when (openPicker) {
        OpenPicker.NONE -> {}

        OpenPicker.DATE -> {
            EntryDatePicker(state.date, { actions.onDateChange(it).also { closePicker() } }, closePicker)
        }

        OpenPicker.TIME -> {
            EntryTimePicker(state.time, is24Hour, { actions.onTimeChange(it).also { closePicker() } }, closePicker)
        }
    }
}

@Composable
private fun FieldButton(
    text: String,
    tag: String,
    onClick: () -> Unit,
) {
    OutlinedButton(onClick, Modifier.fillMaxWidth().heightIn(min = MinTouchTarget).testTag(tag)) { Text(text) }
}

private fun EntryTimeError.messageRes(): Int =
    when (this) {
        EntryTimeError.IN_FUTURE -> R.string.trackers_event_error_future
        EntryTimeError.BEFORE_TRACKER_START -> R.string.trackers_event_error_before_start
    }
