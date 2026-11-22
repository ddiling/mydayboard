package io.github.ddiling.mydayboard.feature.tasks.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import io.github.ddiling.mydayboard.core.designsystem.theme.TaskColors
import io.github.ddiling.mydayboard.core.domain.task.RowState
import io.github.ddiling.mydayboard.core.domain.task.TaskRow
import io.github.ddiling.mydayboard.feature.tasks.R
import java.time.LocalDate

private val MinRowHeight = 48.dp

/**
 * One task on one line: the name, small grey details under it, and on the right the count ("1/3") or, for a
 * done task, a tick. A done task is also grey and struck through, and a late task's details are red and say
 * "late", so colour is never the only sign. The row is 48 dp high even if the text is small, and grows when the
 * font does. It is named `TaskRowLine` because the domain already has a `TaskRow`; the list and the dashboard
 * share it so a task looks the same in both. [today] is the day the row was judged on.
 */
@Composable
internal fun TaskRowLine(
    row: TaskRow,
    today: LocalDate,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val done = row.state == RowState.DONE
    val details = detailsText(LocalResources.current, row, today)
    val detailsColor = if (row.state == RowState.LATE) TaskColors.late else MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = MinRowHeight)
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp)
            .semantics(mergeDescendants = true) {}
            .testTag("tasks_list_row_${row.task.id}"),
        Arrangement.spacedBy(12.dp),
        Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                row.task.name,
                style = MaterialTheme.typography.bodyMedium,
                color = if (done) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                textDecoration = if (done) TextDecoration.LineThrough else null,
            )
            if (details.isNotEmpty()) {
                Text(details, style = MaterialTheme.typography.bodySmall, color = detailsColor)
            }
        }
        if (done) {
            Icon(
                Icons.Default.Check,
                contentDescription = stringResource(R.string.tasks_row_done),
                tint = TaskColors.done,
            )
        } else if (row.target > 1 && row.state != RowState.UPCOMING) {
            Text(
                "${row.done}/${row.target}",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.testTag("tasks_list_count_${row.task.id}"),
            )
        }
    }
}
