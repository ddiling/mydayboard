package io.github.ddiling.mydayboard.feature.tasks.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import io.github.ddiling.mydayboard.core.domain.task.RowState
import io.github.ddiling.mydayboard.core.domain.task.TaskRow
import io.github.ddiling.mydayboard.feature.tasks.R

private val LateRed = Color(0xFFFF8A80)
private val SwipeGreen = Color(0xFF1B5E20)

/** One task row; swipe right = done. The row snaps back: the data flow redraws it in its new state. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TaskRowView(
    row: TaskRow,
    onSwipeDone: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state =
        rememberSwipeToDismissBoxState(
            confirmValueChange = {
                if (it == SwipeToDismissBoxValue.StartToEnd) onSwipeDone()
                false
            },
        )
    SwipeToDismissBox(
        state = state,
        enableDismissFromEndToStart = false,
        modifier = modifier,
        backgroundContent = { Box(Modifier.fillMaxWidth().heightIn(min = 52.dp).background(SwipeGreen)) },
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .clickable(onClick = onClick)
                .heightIn(min = 52.dp)
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val done = row.state == RowState.DONE
            Text(
                row.task.name,
                Modifier.weight(1f),
                color = if (row.state == RowState.LATE) LateRed else MaterialTheme.colorScheme.onSurface,
                textDecoration = if (done) TextDecoration.LineThrough else null,
            )
            if (row.target > 1) Text("${row.done}/${row.target}", Modifier.padding(start = 8.dp))
            row.time?.let { Text(it.toString(), Modifier.padding(start = 8.dp), MaterialTheme.colorScheme.onSurfaceVariant) }
            if (row.state == RowState.LATE) {
                Text(stringResource(R.string.tasks_late), Modifier.padding(start = 8.dp), LateRed)
            }
        }
    }
}
