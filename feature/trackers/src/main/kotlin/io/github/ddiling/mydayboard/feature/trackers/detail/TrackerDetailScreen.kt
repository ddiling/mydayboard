package io.github.ddiling.mydayboard.feature.trackers.detail

import android.text.format.DateFormat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.ddiling.mydayboard.feature.trackers.R
import io.github.ddiling.mydayboard.feature.trackers.event.EventTimeDialog
import io.github.ddiling.mydayboard.feature.trackers.event.EventTimeDialogActions
import io.github.ddiling.mydayboard.feature.trackers.event.EventTimeEditorState
import io.github.ddiling.mydayboard.feature.trackers.reason.ReasonPromptActions
import io.github.ddiling.mydayboard.feature.trackers.reason.ReasonPromptDialog

/** Hilt provides the ViewModel; the callbacks keep navigation out of the feature's screens. */
@Composable
internal fun TrackerDetailRoute(
    onEditClick: () -> Unit,
    onBack: () -> Unit,
    onAddFirstReason: (trackerId: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TrackerDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val prompt by viewModel.prompt.collectAsStateWithLifecycle()
    val actions =
        TrackerDetailActions(
            onPlusOne = viewModel::onPlusOne,
            onReset = viewModel::onReset,
            onUndo = viewModel::onUndo,
            onUndoDismissed = viewModel::onUndoDismissed,
            onEditClick = onEditClick,
            onBack = onBack,
            onAddPastEntry = viewModel.entryEditor::openAdd,
            onEditEntry = viewModel.entryEditor::openEdit,
            entryDialog =
                EventTimeDialogActions(
                    onDateChange = viewModel.entryEditor::changeDate,
                    onTimeChange = viewModel.entryEditor::changeTime,
                    onSave = viewModel.entryEditor::save,
                    onDelete = viewModel.entryEditor::delete,
                    onDismiss = viewModel.entryEditor::dismiss,
                ),
        )
    TrackerDetailScreen(uiState, actions, modifier)
    val promptActions =
        ReasonPromptActions(
            onWait = viewModel.prompter::dismiss,
            onAnother = viewModel.prompter::another,
            onLogAnyway = viewModel::onPromptLogAnyway,
            onAddFirstReason = { trackerId ->
                viewModel.onPromptAddFirstReason()
                onAddFirstReason(trackerId)
            },
        )
    ReasonPromptDialog(prompt, promptActions)
}

/** The +1 or Reset button stays reachable while the lists scroll; the undo offer appears as a snackbar. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TrackerDetailScreen(
    uiState: TrackerDetailUiState,
    actions: TrackerDetailActions,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    UndoSnackbar(uiState.pendingUndo(), snackbarHostState, actions.onUndo, actions.onUndoDismissed)
    uiState.editor()?.let { EventTimeDialog(it, DateFormat.is24HourFormat(LocalContext.current), actions.entryDialog) }
    Scaffold(
        modifier = modifier,
        topBar = {
            DetailTopBar(uiState, actions)
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            when (uiState) {
                is TrackerDetailUiState.Counter -> {
                    ExtendedFloatingActionButton(onClick = actions.onPlusOne) {
                        Text(stringResource(R.string.trackers_detail_plus_one))
                    }
                }

                is TrackerDetailUiState.DaysSince -> {
                    ExtendedFloatingActionButton(
                        onClick = actions.onReset,
                        modifier = Modifier.testTag("trackers_detail_reset"),
                    ) { Text(stringResource(R.string.trackers_detail_reset)) }
                }

                TrackerDetailUiState.Loading, TrackerDetailUiState.NotFound -> {}
            }
        },
    ) { padding ->
        val contentModifier = Modifier.fillMaxSize().padding(padding)
        when (uiState) {
            TrackerDetailUiState.Loading -> {
                Box(contentModifier, contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            }

            TrackerDetailUiState.NotFound -> {
                NotFound(actions.onBack, contentModifier)
            }

            is TrackerDetailUiState.Counter -> {
                CounterContent(uiState, actions.onEditEntry, contentModifier)
            }

            is TrackerDetailUiState.DaysSince -> {
                DaysSinceContent(uiState, actions.onEditEntry, contentModifier)
            }
        }
    }
}

private fun TrackerDetailUiState.pendingUndo(): Undo? =
    when (this) {
        is TrackerDetailUiState.Counter -> pendingUndo
        is TrackerDetailUiState.DaysSince -> pendingUndo
        TrackerDetailUiState.Loading, TrackerDetailUiState.NotFound -> null
    }

private fun TrackerDetailUiState.editor(): EventTimeEditorState? =
    when (this) {
        is TrackerDetailUiState.Counter -> editor
        is TrackerDetailUiState.DaysSince -> editor
        TrackerDetailUiState.Loading, TrackerDetailUiState.NotFound -> null
    }

private fun TrackerDetailUiState.title(): String =
    when (this) {
        is TrackerDetailUiState.Counter -> name
        is TrackerDetailUiState.DaysSince -> name
        TrackerDetailUiState.Loading, TrackerDetailUiState.NotFound -> ""
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DetailTopBar(
    uiState: TrackerDetailUiState,
    actions: TrackerDetailActions,
) {
    TopAppBar(
        title = { Text(uiState.title()) },
        actions = {
            if (uiState is TrackerDetailUiState.Counter || uiState is TrackerDetailUiState.DaysSince) {
                IconButton(onClick = actions.onAddPastEntry, modifier = Modifier.testTag("trackers_detail_add_entry")) {
                    Icon(
                        painterResource(R.drawable.ic_add_entry),
                        contentDescription = stringResource(R.string.trackers_event_add_title),
                    )
                }
                TextButton(onClick = {
                    actions.onUndoDismissed()
                    actions.onEditClick()
                }) { Text(stringResource(R.string.trackers_edit)) }
            }
        },
    )
}

@Composable
private fun NotFound(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(stringResource(R.string.trackers_editor_not_found))
        Button(onClick = onBack) { Text(stringResource(R.string.trackers_editor_back)) }
    }
}
