package io.github.ddiling.mydayboard.feature.trackers.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.ddiling.mydayboard.feature.trackers.R

/** Hilt provides the ViewModel; the callbacks keep navigation out of the feature's screens. */
@Composable
internal fun TrackerDetailRoute(
    onEditClick: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TrackerDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val actions =
        TrackerDetailActions(
            onPlusOne = viewModel::onPlusOne,
            onReset = viewModel::onReset,
            onUndo = viewModel::onUndo,
            onUndoDismissed = viewModel::onUndoDismissed,
            onEditClick = onEditClick,
            onBack = onBack,
        )
    TrackerDetailScreen(uiState, actions, modifier)
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
    UndoSnackbar(uiState.pendingUndo(), snackbarHostState, actions)
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(uiState.title()) },
                actions = {
                    if (uiState is TrackerDetailUiState.Counter || uiState is TrackerDetailUiState.DaysSince) {
                        TextButton(onClick = {
                            actions.onUndoDismissed()
                            actions.onEditClick()
                        }) { Text(stringResource(R.string.trackers_edit)) }
                    }
                },
            )
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
                CounterContent(uiState, contentModifier)
            }

            is TrackerDetailUiState.DaysSince -> {
                DaysSinceContent(uiState, contentModifier)
            }
        }
    }
}

/**
 * Shows the undo offer for as long as [undo] is set. A newer action changes the key, which cancels the old
 * snackbar without reporting it as dismissed (that report would clear the newer action).
 */
@Composable
private fun UndoSnackbar(
    undo: Undo?,
    host: SnackbarHostState,
    actions: TrackerDetailActions,
) {
    val message =
        stringResource(
            when (undo?.kind) {
                UndoKind.RESET -> R.string.trackers_detail_reset_done
                UndoKind.ADDED, null -> R.string.trackers_detail_added
            },
        )
    val undoLabel = stringResource(R.string.trackers_detail_undo)
    val currentActions by rememberUpdatedState(actions)
    LaunchedEffect(undo) {
        if (undo == null) return@LaunchedEffect
        when (host.showSnackbar(message, undoLabel, duration = SnackbarDuration.Long)) {
            SnackbarResult.ActionPerformed -> currentActions.onUndo()
            SnackbarResult.Dismissed -> currentActions.onUndoDismissed()
        }
    }
}

private fun TrackerDetailUiState.pendingUndo(): Undo? =
    when (this) {
        is TrackerDetailUiState.Counter -> pendingUndo
        is TrackerDetailUiState.DaysSince -> pendingUndo
        TrackerDetailUiState.Loading, TrackerDetailUiState.NotFound -> null
    }

private fun TrackerDetailUiState.title(): String =
    when (this) {
        is TrackerDetailUiState.Counter -> name
        is TrackerDetailUiState.DaysSince -> name
        TrackerDetailUiState.Loading, TrackerDetailUiState.NotFound -> ""
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
