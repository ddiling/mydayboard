package io.github.ddiling.mydayboard.feature.tasks.detail

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.ddiling.mydayboard.feature.tasks.day.DaySheetActions

/**
 * Hilt provides the ViewModel; where Edit leads is up to the caller, so the details know no other screen. A
 * history row opens the day sheet.
 */
@Composable
internal fun TaskDetailRoute(
    onBack: () -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TaskDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val actions =
        remember(viewModel, onBack, onEdit) {
            TaskDetailActions(
                onBack = onBack,
                onEdit = onEdit,
                onBigButton = viewModel::onBigButton,
                onChipClick = viewModel::onChipClick,
                onHistoryRowClick = viewModel::onDayClick,
                onUndo = viewModel::onUndo,
                onUndoDismissed = viewModel::onUndoDismissed,
            )
        }
    val daySheet by viewModel.daySheet.state.collectAsStateWithLifecycle()
    val sheet = viewModel.daySheet
    val sheetActions =
        remember(viewModel) {
            DaySheetActions(sheet::setSingle, sheet::toggle, sheet::step, viewModel::onDaySave, sheet::dismiss)
        }
    TaskDetailScreen(uiState, actions, modifier, daySheet, sheetActions)
}
