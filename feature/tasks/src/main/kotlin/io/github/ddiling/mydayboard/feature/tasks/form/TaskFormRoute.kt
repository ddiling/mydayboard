package io.github.ddiling.mydayboard.feature.tasks.form

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/** Hilt provides the ViewModel; the form leaves itself when a save has finished, so no caller has to watch for it. */
@Composable
internal fun TaskFormRoute(
    onBack: () -> Unit,
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
    TaskFormScreen(uiState, categories, viewModel.actions.copy(onClose = onBack), modifier)
}
