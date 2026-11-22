package io.github.ddiling.mydayboard.feature.tasks.categories

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Hilt provides the ViewModel; where Back and a task tap lead is up to the caller. A deleted category leaves
 * through [onBack] too, which is the list the user came from.
 */
@Composable
internal fun CategoryStatsRoute(
    onBack: () -> Unit,
    onTaskClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CategoryStatsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val dialog by viewModel.categoryDialog.collectAsStateWithLifecycle()
    val left by viewModel.left.collectAsStateWithLifecycle()
    LaunchedEffect(left) {
        if (left) {
            viewModel.onLeftHandled()
            onBack()
        }
    }
    val actions =
        remember(viewModel, onBack, onTaskClick) {
            CategoryStatsActions(onBack = onBack, onEdit = viewModel::onEdit, onTaskClick = onTaskClick)
        }
    CategoryStatsScreen(uiState, actions, modifier, dialog, viewModel.categoryDialogActions)
}
