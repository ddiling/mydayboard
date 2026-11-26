package io.github.ddiling.mydayboard.feature.tasks.form

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.ddiling.mydayboard.feature.tasks.R

/** The dialog window around [DeleteTaskContent]; a tap outside and Back both mean "Keep it", the safe answer. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DeleteTaskDialog(
    taskName: String,
    actions: TaskFormActions,
) {
    BasicAlertDialog(onDismissRequest = actions.onDeleteDismiss) { DeleteTaskContent(taskName, actions) }
}

/**
 * The question body, split from the window so previews and tests can show it directly. "Keep it" is the big
 * button and "Delete" the quiet red one, so a hurried tap lands on the safe choice. The whole body scrolls so
 * the buttons stay reachable at a large font.
 */
@Composable
internal fun DeleteTaskContent(
    taskName: String,
    actions: TaskFormActions,
) {
    Surface(shape = AlertDialogDefaults.shape, color = AlertDialogDefaults.containerColor) {
        Column(Modifier.heightIn(max = 560.dp).verticalScroll(rememberScrollState()).padding(24.dp)) {
            Text(stringResource(R.string.tasks_delete_title), style = MaterialTheme.typography.headlineSmall)
            Text(
                stringResource(R.string.tasks_delete_question, taskName),
                Modifier.padding(top = 16.dp),
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                stringResource(R.string.tasks_delete_warning),
                Modifier.padding(top = 8.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(
                onClick = actions.onDeleteDismiss,
                modifier = Modifier.fillMaxWidth().padding(top = 24.dp).testTag("tasks_delete_keep"),
            ) { Text(stringResource(R.string.tasks_delete_keep)) }
            TextButton(
                onClick = actions.onDeleteConfirm,
                modifier = Modifier.fillMaxWidth().testTag("tasks_delete_confirm"),
            ) { Text(stringResource(R.string.tasks_delete_confirm), color = MaterialTheme.colorScheme.error) }
        }
    }
}
