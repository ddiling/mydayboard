package io.github.ddiling.mydayboard.feature.tasks.list

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.res.stringResource
import io.github.ddiling.mydayboard.feature.tasks.R
import io.github.ddiling.mydayboard.feature.tasks.navigation.TasksListNotice

/**
 * Says what a closed screen did: "Pills archived" with Undo, or "Pills deleted" without it, because a delete
 * cannot be undone. [onUndoArchive] gets the task id; [onShown] is called when the message has ended either way,
 * so the notice is not shown a second time.
 */
@Composable
internal fun TasksListNoticeSnackbar(
    notice: TasksListNotice?,
    hostState: SnackbarHostState,
    onUndoArchive: (String) -> Unit,
    onShown: () -> Unit,
) {
    val message =
        when (notice) {
            is TasksListNotice.Archived -> stringResource(R.string.tasks_list_archived_notice, notice.name)
            is TasksListNotice.Deleted -> stringResource(R.string.tasks_list_deleted_notice, notice.name)
            null -> null
        }
    val undoLabel = stringResource(R.string.tasks_undo_action)
    val currentOnUndo = rememberUpdatedState(onUndoArchive)
    val currentOnShown = rememberUpdatedState(onShown)
    LaunchedEffect(notice) {
        if (notice != null && message != null) {
            val label = if (notice is TasksListNotice.Archived) undoLabel else null
            val result = hostState.showSnackbar(message, label, duration = SnackbarDuration.Short)
            if (result == SnackbarResult.ActionPerformed && notice is TasksListNotice.Archived) {
                currentOnUndo.value(notice.id)
            }
            currentOnShown.value()
        }
    }
}
