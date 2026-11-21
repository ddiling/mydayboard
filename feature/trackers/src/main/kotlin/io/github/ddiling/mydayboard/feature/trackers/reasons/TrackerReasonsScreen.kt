package io.github.ddiling.mydayboard.feature.trackers.reasons

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.ddiling.mydayboard.core.domain.tracker.TrackerReason
import io.github.ddiling.mydayboard.feature.trackers.R

private val SideEdge = 20.dp
private val RowMinHeight = 56.dp
private val FabClearance = 88.dp

/** What the reasons screen can do; the defaults let previews and tests pass only what they care about. */
internal data class TrackerReasonsActions(
    val onBack: () -> Unit = {},
    val onAddClick: () -> Unit = {},
    val onEditClick: (String) -> Unit = {},
    val onDelete: (String) -> Unit = {},
    val onUndo: () -> Unit = {},
    val onUndoDismissed: () -> Unit = {},
    val onTextChange: (String) -> Unit = {},
    val onSave: () -> Unit = {},
    val onDismissEditor: () -> Unit = {},
)

/** Hilt provides the ViewModel; the callback keeps navigation out of the feature's screens. */
@Composable
internal fun TrackerReasonsRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TrackerReasonsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val actions =
        TrackerReasonsActions(
            onBack = onBack,
            onAddClick = viewModel::onAddClick,
            onEditClick = viewModel::onEditClick,
            onDelete = viewModel::onSwipeDelete,
            onUndo = viewModel::onUndoDelete,
            onUndoDismissed = viewModel::onUndoDismissed,
            onTextChange = viewModel::onTextChange,
            onSave = viewModel::onSave,
            onDismissEditor = viewModel::onDismissEditor,
        )
    TrackerReasonsScreen(uiState, actions, modifier)
}

/** The add button stays reachable while the list scrolls; the undo offer for a delete appears as a snackbar. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TrackerReasonsScreen(
    uiState: TrackerReasonsUiState,
    actions: TrackerReasonsActions,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val content = uiState as? TrackerReasonsUiState.Content
    DeleteUndoSnackbar(content?.pendingUndoId, snackbarHostState, actions.onUndo, actions.onUndoDismissed)
    content?.editor?.let { ReasonEditDialog(it, actions.onTextChange, actions.onSave, actions.onDismissEditor) }
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { ReasonsTitle(content?.trackerName) },
                navigationIcon = {
                    IconButton(onClick = actions.onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.trackers_reasons_back),
                        )
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (content != null) {
                ExtendedFloatingActionButton(onClick = actions.onAddClick) {
                    Text(stringResource(R.string.trackers_reasons_add))
                }
            }
        },
    ) { padding ->
        val body = Modifier.fillMaxSize().padding(padding)
        when (uiState) {
            TrackerReasonsUiState.Loading -> {
                Box(body, contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            }

            TrackerReasonsUiState.NotFound -> {
                NotFound(actions.onBack, body)
            }

            is TrackerReasonsUiState.Content -> {
                if (uiState.reasons.isEmpty()) {
                    Box(body.padding(horizontal = SideEdge), contentAlignment = Alignment.Center) {
                        Text(
                            stringResource(R.string.trackers_reasons_empty),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    ReasonList(uiState.reasons, actions, body)
                }
            }
        }
    }
}

@Composable
private fun ReasonsTitle(trackerName: String?) {
    Column {
        Text(stringResource(R.string.trackers_reasons_title))
        if (trackerName != null) {
            Text(trackerName, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ReasonList(
    reasons: List<TrackerReason>,
    actions: TrackerReasonsActions,
    modifier: Modifier,
) {
    // The bottom padding keeps the last row above the add button.
    LazyColumn(modifier.testTag("trackers_reasons_list"), contentPadding = PaddingValues(bottom = FabClearance)) {
        items(reasons, key = { it.id }) { reason ->
            SwipeableReasonRow(
                reason,
                onClick = { actions.onEditClick(reason.id) },
                onDelete = { actions.onDelete(reason.id) },
            )
        }
    }
}

/**
 * Swiping from the start deletes the reason. Swiping is hard for some users, so the row also offers the same
 * delete as a named accessibility action that screen readers list.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableReasonRow(
    reason: TrackerReason,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    val currentOnDelete by rememberUpdatedState(onDelete)
    val swipeState = rememberSwipeToDismissBoxState()
    // The row leaves the list once the delete is reported, so this runs once per swipe.
    LaunchedEffect(swipeState.currentValue) {
        if (swipeState.currentValue == SwipeToDismissBoxValue.StartToEnd) currentOnDelete()
    }
    val deleteLabel = stringResource(R.string.trackers_reasons_delete)
    SwipeToDismissBox(
        state = swipeState,
        enableDismissFromEndToStart = false,
        backgroundContent = {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        MaterialTheme.colorScheme.errorContainer,
                    ).padding(horizontal = SideEdge),
            ) {
                Text(
                    deleteLabel,
                    Modifier.align(Alignment.CenterStart),
                    MaterialTheme.colorScheme.onErrorContainer,
                    fontSize = 14.sp,
                )
            }
        },
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .heightIn(min = RowMinHeight)
                .clickable(onClick = onClick)
                .semantics {
                    customActions =
                        listOf(
                            CustomAccessibilityAction(deleteLabel) {
                                onDelete()
                                true
                            },
                        )
                }.padding(horizontal = SideEdge, vertical = 12.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(reason.text, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }
    }
}

/** Offers Undo for as long as [reasonId] is set; a newer delete changes the key and replaces the old offer. */
@Composable
private fun DeleteUndoSnackbar(
    reasonId: String?,
    host: SnackbarHostState,
    onUndo: () -> Unit,
    onUndoDismissed: () -> Unit,
) {
    val message = stringResource(R.string.trackers_reasons_deleted)
    val undoLabel = stringResource(R.string.trackers_detail_undo)
    val currentOnUndo by rememberUpdatedState(onUndo)
    val currentOnDismissed by rememberUpdatedState(onUndoDismissed)
    LaunchedEffect(reasonId) {
        if (reasonId == null) return@LaunchedEffect
        when (host.showSnackbar(message, undoLabel, duration = SnackbarDuration.Long)) {
            SnackbarResult.ActionPerformed -> currentOnUndo()
            SnackbarResult.Dismissed -> currentOnDismissed()
        }
    }
}

@Composable
private fun NotFound(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier.padding(SideEdge),
        Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        Alignment.CenterHorizontally,
    ) {
        Text(stringResource(R.string.trackers_editor_not_found))
        Button(onClick = onBack) { Text(stringResource(R.string.trackers_reasons_not_found_back)) }
    }
}
