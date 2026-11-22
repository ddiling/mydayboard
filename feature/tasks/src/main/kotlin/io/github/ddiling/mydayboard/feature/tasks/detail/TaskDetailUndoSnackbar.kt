package io.github.ddiling.mydayboard.feature.tasks.detail

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.res.stringResource
import io.github.ddiling.mydayboard.feature.tasks.R
import io.github.ddiling.mydayboard.feature.tasks.common.TaskUndoSnackbar

/**
 * Shows the message of [undo]: a tick reuses the list's message ("Pills done", "Send CVs 2/3"), an untick says
 * "Pills unticked". Both end in [onUndo] or [onDismissed]; a newer message replaces the shown one without either.
 */
@Composable
internal fun TaskDetailUndoSnackbar(
    undo: DetailUndo?,
    hostState: SnackbarHostState,
    onUndo: () -> Unit,
    onDismissed: () -> Unit,
) {
    TaskUndoSnackbar((undo as? DetailUndo.Ticked)?.offer, hostState, onUndo) { onDismissed() }
    val unticked = undo as? DetailUndo.Unticked
    val message = unticked?.let { stringResource(R.string.tasks_detail_unticked, it.name) }
    val actionLabel = stringResource(R.string.tasks_undo_action)
    val currentOnUndo = rememberUpdatedState(onUndo)
    val currentOnDismissed = rememberUpdatedState(onDismissed)
    LaunchedEffect(unticked) {
        if (message != null) {
            when (hostState.showSnackbar(message, actionLabel, duration = SnackbarDuration.Short)) {
                SnackbarResult.ActionPerformed -> currentOnUndo.value()
                SnackbarResult.Dismissed -> currentOnDismissed.value()
            }
        }
    }
}
