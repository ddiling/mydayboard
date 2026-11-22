package io.github.ddiling.mydayboard.feature.tasks.detail

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.LocalDate

/**
 * Hilt provides the ViewModel; where Edit and a history row lead is up to the caller, so the details know no
 * other screen. [onHistoryRowClick] is a no-op until the day sheet exists.
 */
@Composable
internal fun TaskDetailRoute(
    onBack: () -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier,
    onHistoryRowClick: (LocalDate) -> Unit = {},
    viewModel: TaskDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val actions =
        remember(viewModel, onBack, onEdit, onHistoryRowClick) {
            TaskDetailActions(
                onBack = onBack,
                onEdit = onEdit,
                onBigButton = viewModel::onBigButton,
                onChipClick = viewModel::onChipClick,
                onHistoryRowClick = onHistoryRowClick,
                onUndo = viewModel::onUndo,
                onUndoDismissed = viewModel::onUndoDismissed,
            )
        }
    TaskDetailScreen(uiState, actions, modifier)
}
