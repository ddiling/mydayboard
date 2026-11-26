package io.github.ddiling.mydayboard.feature.tasks.form

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.ddiling.mydayboard.feature.tasks.categories.CategoryDialog
import io.github.ddiling.mydayboard.feature.tasks.categories.CategoryDialogState
import io.github.ddiling.mydayboard.feature.tasks.navigation.TasksListNotice

/**
 * Hilt provides the ViewModel; the form leaves itself when a save has finished, so no caller has to watch for it.
 * After an archive or delete it leaves through [onFinish] instead, so the list can show what happened.
 */
@Composable
internal fun TaskFormRoute(
    onBack: () -> Unit,
    onFinish: (TasksListNotice) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TaskFormViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    LaunchedEffect(uiState.saved) {
        if (uiState.saved) {
            viewModel.onSavedHandled()
            onBack()
        }
    }
    LaunchedEffect(uiState.finished) {
        uiState.finished?.let {
            viewModel.onFinishedHandled()
            onFinish(it)
        }
    }
    val categoryDialog by viewModel.categoryDialog.collectAsStateWithLifecycle()
    (categoryDialog as? CategoryDialogState.Editing)?.let { CategoryDialog(it, viewModel.categoryDialogActions) }
    uiState.deleteDialogName?.let { DeleteTaskDialog(it, viewModel.actions) }
    TaskFormScreen(uiState, categories, viewModel.actions.copy(onClose = onBack), modifier)
}
