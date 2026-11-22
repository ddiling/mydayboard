package io.github.ddiling.mydayboard.feature.tasks.categories

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
import io.github.ddiling.mydayboard.feature.tasks.form.DashedTextField

/** The dialog window around [CategoryDialogContent]; a tap outside and Back count as Cancel. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CategoryDialog(
    state: CategoryDialogState.Editing,
    actions: CategoryDialogActions,
) {
    BasicAlertDialog(onDismissRequest = actions.onCancel) { CategoryDialogContent(state, actions) }
}

/**
 * The dialog body, split from the window so previews and tests can show it directly. The whole body scrolls, not
 * only the field: at a large font the buttons would otherwise be pushed out of reach. Delete is only offered for
 * an existing category.
 */
@Composable
internal fun CategoryDialogContent(
    state: CategoryDialogState.Editing,
    actions: CategoryDialogActions,
) {
    Surface(shape = AlertDialogDefaults.shape, color = AlertDialogDefaults.containerColor) {
        Column(
            Modifier
                .heightIn(max = 560.dp)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
        ) {
            val title = if (state.id == null) R.string.tasks_category_title_new else R.string.tasks_category_title_edit
            Text(
                stringResource(title),
                Modifier.padding(bottom = 16.dp),
                style = MaterialTheme.typography.headlineSmall,
            )
            DashedTextField(
                label = stringResource(R.string.tasks_category_name),
                value = state.name,
                onValueChange = actions.onNameChange,
                error = state.error?.message(),
                modifier = Modifier.testTag("tasks_category_name"),
            )
            Button(
                onClick = actions.onSave,
                enabled = state.canSave,
                modifier = Modifier.fillMaxWidth().padding(top = 24.dp).testTag("tasks_category_save"),
            ) { Text(stringResource(R.string.tasks_category_save)) }
            TextButton(
                onClick = actions.onCancel,
                modifier = Modifier.fillMaxWidth().testTag("tasks_category_cancel"),
            ) {
                Text(stringResource(R.string.tasks_category_cancel), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (state.id != null) {
                TextButton(
                    onClick = actions.onDelete,
                    modifier = Modifier.fillMaxWidth().testTag("tasks_category_delete"),
                ) {
                    Text(stringResource(R.string.tasks_category_delete), color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
private fun CategoryNameError.message(): String =
    when (this) {
        CategoryNameError.Duplicate -> stringResource(R.string.tasks_category_error_duplicate)
        is CategoryNameError.TooLong -> stringResource(R.string.tasks_category_error_too_long, max)
    }
