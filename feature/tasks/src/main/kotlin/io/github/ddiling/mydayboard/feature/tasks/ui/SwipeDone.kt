package io.github.ddiling.mydayboard.feature.tasks.ui

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.res.stringResource
import io.github.ddiling.mydayboard.feature.tasks.R
import kotlinx.coroutines.launch

/** Returns a handler: ticks the task, then offers Undo on [snackbar]. */
@Composable
internal fun rememberSwipeDone(
    viewModel: TasksOverviewViewModel,
    snackbar: SnackbarHostState,
): (String) -> Unit {
    val scope = rememberCoroutineScope()
    val message = stringResource(R.string.tasks_done_snackbar)
    val undo = stringResource(R.string.tasks_undo)
    return { id ->
        scope.launch {
            val outcome = viewModel.swipeDone(id)
            if (outcome != null) {
                snackbar.currentSnackbarData?.dismiss()
                val result = snackbar.showSnackbar(message, undo, duration = SnackbarDuration.Short)
                if (result == SnackbarResult.ActionPerformed) viewModel.undo(outcome)
            }
        }
    }
}
