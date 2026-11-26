package io.github.ddiling.mydayboard.feature.tasks.detail

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import io.github.ddiling.mydayboard.feature.tasks.R
import io.github.ddiling.mydayboard.feature.tasks.common.TaskUndoSnackbar
import io.github.ddiling.mydayboard.feature.tasks.day.dayMarkedText

/**
 * Shows the message of [undo]: a tick reuses the list's message ("Pills done", "Send CVs 2/3"), an untick says
 * "Pills unticked", a saved past day says "Lunch · Sat 21.11 marked as done". All end in [onUndo] or
 * [onDismissed]; a newer message replaces the shown one without either.
 */
@Composable
internal fun TaskDetailUndoSnackbar(
    undo: DetailUndo?,
    hostState: SnackbarHostState,
    onUndo: () -> Unit,
    onDismissed: () -> Unit,
) {
    TaskUndoSnackbar((undo as? DetailUndo.Ticked)?.offer, hostState, onUndo) { onDismissed() }
    val other = undo?.takeUnless { it is DetailUndo.Ticked }
    val resources = LocalResources.current
    val message =
        when (other) {
            is DetailUndo.Unticked -> stringResource(R.string.tasks_detail_unticked, other.name)
            is DetailUndo.Marked -> dayMarkedText(resources, other.marked)
            else -> null
        }
    val actionLabel = stringResource(R.string.tasks_undo_action)
    val currentOnUndo = rememberUpdatedState(onUndo)
    val currentOnDismissed = rememberUpdatedState(onDismissed)
    LaunchedEffect(other) {
        if (message != null) {
            when (hostState.showSnackbar(message, actionLabel, duration = SnackbarDuration.Short)) {
                SnackbarResult.ActionPerformed -> currentOnUndo.value()
                SnackbarResult.Dismissed -> currentOnDismissed.value()
            }
        }
    }
}
