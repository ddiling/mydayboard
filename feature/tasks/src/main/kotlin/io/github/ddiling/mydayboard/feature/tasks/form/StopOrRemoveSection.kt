package io.github.ddiling.mydayboard.feature.tasks.form

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.ddiling.mydayboard.feature.tasks.R

/**
 * The two rare, final actions of an existing task, kept on the calm edit screen and not in a menu next to a tick.
 * Archive acts at once (the list offers Undo); delete only asks, because it cannot be undone.
 */
@Composable
internal fun StopOrRemoveSection(
    actions: TaskFormActions,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        Text(
            stringResource(R.string.tasks_form_stop_title),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        val normal = MaterialTheme.colorScheme.onSurface
        val danger = MaterialTheme.colorScheme.error
        StopRow(R.string.tasks_form_archive, "tasks_form_archive", normal, actions.onArchive)
        StopRow(R.string.tasks_form_delete, "tasks_form_delete", danger, actions.onDeleteRequest)
    }
}

@Composable
private fun StopRow(
    label: Int,
    tag: String,
    color: Color,
    onClick: () -> Unit,
) {
    Text(
        stringResource(label),
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .clickable(role = Role.Button, onClick = onClick)
                .padding(vertical = 12.dp)
                .testTag(tag),
        color = color,
        style = MaterialTheme.typography.bodyLarge,
        textAlign = TextAlign.Start,
    )
}
