package io.github.ddiling.mydayboard.feature.tasks.form

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.ddiling.mydayboard.feature.tasks.R

/** Placeholder until the form screen exists; it already leaves the form when a save has finished. */
@Composable
internal fun TaskFormRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TaskFormViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(uiState.saved) {
        if (uiState.saved) {
            viewModel.onSavedHandled()
            onBack()
        }
    }
    Text(text = stringResource(R.string.tasks_form_placeholder), modifier = modifier)
}
