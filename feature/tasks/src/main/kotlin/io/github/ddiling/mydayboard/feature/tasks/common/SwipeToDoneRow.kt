package io.github.ddiling.mydayboard.feature.tasks.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.ddiling.mydayboard.core.designsystem.theme.TaskColors
import io.github.ddiling.mydayboard.feature.tasks.R
import kotlinx.coroutines.launch

/**
 * Swipe right to mark done, with a green "Done" label under the finger (shown only while swiping, so the row
 * is plain the rest of the time). The row always springs back: the
 * change shows through the data (the row turns done or its count goes up), never through the row staying
 * swiped away, so one swipe is one step and the row is ready for the next. The
 * same action is offered to screen readers as a custom "Mark done" action, because a swipe is hard to make
 * with assistive tools; [enabled] false (a task that is not due) turns off both.
 */
@Composable
internal fun SwipeToDoneRow(
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    val currentOnDone = rememberUpdatedState(onDone)
    val state = rememberSwipeToDismissBoxState()
    val scope = rememberCoroutineScope()
    // One stable callback: a new one on each recomposition would restart the box's effect and report twice.
    val onDismiss =
        remember(state, scope) {
            { target: SwipeToDismissBoxValue ->
                if (target == SwipeToDismissBoxValue.StartToEnd) {
                    currentOnDone.value()
                    scope.launch { state.reset() }
                }
            }
        }
    val markDone = stringResource(R.string.tasks_row_mark_done)
    SwipeToDismissBox(
        state = state,
        backgroundContent = {
            if (enabled && state.dismissDirection == SwipeToDismissBoxValue.StartToEnd) DoneLabel()
        },
        modifier =
            modifier.semantics {
                if (enabled) {
                    customActions =
                        listOf(
                            CustomAccessibilityAction(markDone) {
                                currentOnDone.value()
                                true
                            },
                        )
                }
            },
        enableDismissFromEndToStart = false,
        enableDismissFromStartToEnd = enabled,
        onDismiss = onDismiss,
        content = { Box(Modifier.background(MaterialTheme.colorScheme.background)) { content() } },
    )
}

@Composable
private fun DoneLabel() {
    Row(
        Modifier.fillMaxSize().padding(horizontal = 20.dp),
        Arrangement.spacedBy(8.dp),
        Alignment.CenterVertically,
    ) {
        Icon(Icons.Default.Check, contentDescription = null, tint = TaskColors.done)
        Text(
            stringResource(R.string.tasks_row_done_label),
            style = MaterialTheme.typography.bodyMedium,
            color = TaskColors.done,
        )
    }
}
