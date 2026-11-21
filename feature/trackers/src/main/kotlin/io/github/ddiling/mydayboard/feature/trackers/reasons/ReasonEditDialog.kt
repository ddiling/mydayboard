package io.github.ddiling.mydayboard.feature.trackers.reasons

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.ddiling.mydayboard.core.domain.tracker.ReasonTextValidation
import io.github.ddiling.mydayboard.feature.trackers.R

/**
 * Add / edit dialog. The counter is always visible, so the user sees the limit before reaching it, and the
 * helper line turns into the error when the text is too long. Save is disabled while the text cannot be stored.
 */
@Composable
internal fun ReasonEditDialog(
    editor: ReasonEditor,
    onTextChange: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
) {
    val tooLong = editor.validation as? ReasonTextValidation.TooLong
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(
                    if (editor.reasonId ==
                        null
                    ) {
                        R.string.trackers_reasons_add
                    } else {
                        R.string.trackers_reasons_edit_title
                    },
                ),
            )
        },
        text = {
            OutlinedTextField(
                value = editor.text,
                onValueChange = onTextChange,
                modifier = Modifier.fillMaxWidth().testTag("trackers_reasons_field"),
                label = { Text(stringResource(R.string.trackers_reasons_field)) },
                isError = tooLong != null,
                supportingText = {
                    Row(Modifier.fillMaxWidth()) {
                        Text(
                            tooLong?.let { stringResource(R.string.trackers_reasons_too_long, it.max) }
                                ?: stringResource(R.string.trackers_reasons_helper),
                            Modifier.weight(1f),
                        )
                        Text(
                            stringResource(R.string.trackers_reasons_counter, editor.length, editor.maxLength),
                            Modifier.padding(start = 8.dp),
                        )
                    }
                },
            )
        },
        confirmButton = {
            TextButton(
                onClick = onSave,
                enabled = editor.canSave,
            ) { Text(stringResource(R.string.trackers_editor_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.trackers_editor_cancel)) }
        },
    )
}
