package io.github.ddiling.mydayboard.feature.trackers.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.ddiling.mydayboard.core.domain.tracker.TrackerNameValidation
import io.github.ddiling.mydayboard.core.domain.tracker.TrackerType
import io.github.ddiling.mydayboard.feature.trackers.R
import java.time.LocalDate

/** Hilt provides the ViewModel; [onResult] lets navigation decide where each way of leaving goes. */
@Composable
internal fun TrackerEditorRoute(
    onResult: (EditorResult, isEditing: Boolean) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TrackerEditorViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(uiState.result) { uiState.result?.let { onResult(it, uiState.isEditing) } }
    val actions =
        TrackerEditorActions(
            viewModel::onNameChange,
            viewModel::onTypeChange,
            viewModel::onStartDateChange,
            viewModel::onSave,
            viewModel::onDeleteClick,
            viewModel::onDeleteConfirm,
            viewModel::onDeleteDismiss,
            onBack,
        )
    TrackerEditorScreen(uiState, actions, modifier)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TrackerEditorScreen(
    uiState: TrackerEditorUiState,
    actions: TrackerEditorActions,
    modifier: Modifier = Modifier,
) {
    val title = if (uiState.isEditing) R.string.trackers_editor_edit_title else R.string.trackers_editor_new_title
    Scaffold(modifier = modifier, topBar = { TopAppBar(title = { Text(stringResource(title)) }) }) { padding ->
        val contentModifier = Modifier.fillMaxSize().padding(padding)
        when (uiState.load) {
            EditorLoad.Loading -> Box(contentModifier)
            EditorLoad.NotFound -> NotFound(actions.onBack, contentModifier)
            EditorLoad.Ready -> Form(uiState, actions, contentModifier)
        }
    }
    if (uiState.showDeleteConfirmation) DeleteDialog(actions.onDeleteConfirm, actions.onDeleteDismiss)
}

@Composable
private fun NotFound(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier.padding(24.dp),
        Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        Alignment.CenterHorizontally,
    ) {
        Text(stringResource(R.string.trackers_editor_not_found))
        Button(onClick = onBack) { Text(stringResource(R.string.trackers_editor_back)) }
    }
}

@Composable
private fun Form(
    uiState: TrackerEditorUiState,
    actions: TrackerEditorActions,
    modifier: Modifier = Modifier,
) {
    Column(modifier.verticalScroll(rememberScrollState()).padding(16.dp), Arrangement.spacedBy(16.dp)) {
        OutlinedTextField(
            value = uiState.name,
            onValueChange = actions.onNameChange,
            label = { Text(stringResource(R.string.trackers_editor_name_label)) },
            isError = uiState.nameError != null,
            supportingText = uiState.nameError?.let { { Text(it.message()) } },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { actions.onSave() }),
            modifier = Modifier.fillMaxWidth(),
        )
        if (!uiState.isEditing) {
            TypeSelector(uiState.type, actions.onTypeChange)
            if (uiState.type == TrackerType.DAYS_SINCE) {
                StartDateField(uiState.startDate, uiState.today, actions.onStartDateChange)
            }
        }
        Button(onClick = actions.onSave, enabled = uiState.canSave, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.trackers_editor_save))
        }
        if (uiState.isEditing) {
            OutlinedButton(onClick = actions.onDeleteClick, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.trackers_editor_delete))
            }
        }
    }
}

@Composable
private fun TrackerNameValidation.message(): String =
    when (this) {
        is TrackerNameValidation.TooLong -> stringResource(R.string.trackers_editor_name_too_long, max)
        else -> stringResource(R.string.trackers_editor_name_blank)
    }

@Composable
private fun TypeSelector(
    selected: TrackerType,
    onSelect: (TrackerType) -> Unit,
) {
    val labels =
        listOf(
            TrackerType.EVENT_COUNTER to R.string.trackers_editor_type_counter,
            TrackerType.DAYS_SINCE to R.string.trackers_editor_type_days_since,
        )
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        labels.forEachIndexed { index, (type, label) ->
            SegmentedButton(
                selected = type == selected,
                onClick = { onSelect(type) },
                shape = SegmentedButtonDefaults.itemShape(index, labels.size),
            ) { Text(stringResource(label)) }
        }
    }
}

@Composable
private fun DeleteDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.trackers_editor_delete_title)) },
        text = { Text(stringResource(R.string.trackers_editor_delete_message)) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(stringResource(R.string.trackers_editor_delete)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.trackers_editor_cancel)) } },
    )
}
