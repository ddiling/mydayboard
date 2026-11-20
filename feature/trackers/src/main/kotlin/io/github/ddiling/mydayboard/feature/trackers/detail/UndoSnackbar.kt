package io.github.ddiling.mydayboard.feature.trackers.detail

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.res.stringResource
import io.github.ddiling.mydayboard.feature.trackers.R

/**
 * Shows the undo offer for as long as [undo] is set. A newer action changes the key, which cancels the old
 * snackbar without reporting it as dismissed (that report would clear the newer action). Shared by the detail
 * screen and the dashboard card so both word and time the offer the same way.
 */
@Composable
internal fun UndoSnackbar(
    undo: Undo?,
    host: SnackbarHostState,
    onUndo: () -> Unit,
    onUndoDismissed: () -> Unit,
) {
    val message =
        stringResource(
            when (undo?.kind) {
                UndoKind.RESET -> R.string.trackers_detail_reset_done
                UndoKind.DELETED -> R.string.trackers_detail_entry_deleted
                UndoKind.ADDED, null -> R.string.trackers_detail_added
            },
        )
    val undoLabel = stringResource(R.string.trackers_detail_undo)
    val currentOnUndo by rememberUpdatedState(onUndo)
    val currentOnDismissed by rememberUpdatedState(onUndoDismissed)
    LaunchedEffect(undo) {
        if (undo == null) return@LaunchedEffect
        when (host.showSnackbar(message, undoLabel, duration = SnackbarDuration.Long)) {
            SnackbarResult.ActionPerformed -> currentOnUndo()
            SnackbarResult.Dismissed -> currentOnDismissed()
        }
    }
}
