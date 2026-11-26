package io.github.ddiling.mydayboard.feature.tasks.list

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/** Hilt provides the ViewModel; where a tap or "+" leads is up to the caller, so the list knows no other screen. */
@Composable
internal fun TasksListRoute(
    onBack: () -> Unit,
    onAddTask: () -> Unit,
    onTaskClick: (String) -> Unit,
    onCategoryStats: (String) -> Unit,
    onArchiveClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TasksListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val actions =
        remember(viewModel, onBack, onAddTask, onTaskClick, onCategoryStats, onArchiveClick) {
            TasksListActions(
                onBack = onBack,
                onAddTask = onAddTask,
                onTaskClick = onTaskClick,
                onCategoryStats = onCategoryStats,
                onSwipeDone = viewModel::onSwipeDone,
                onUndo = viewModel::onUndo,
                onUndoDismissed = viewModel::onUndoDismissed,
                onArchiveClick = onArchiveClick,
                onUndoArchive = viewModel::onUndoArchive,
                onNoticeShown = viewModel::onNoticeShown,
            )
        }
    val notice by viewModel.notice.collectAsStateWithLifecycle()
    TasksListScreen(uiState, actions, modifier, notice)
}
