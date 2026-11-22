package io.github.ddiling.mydayboard.feature.tasks.form

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.ddiling.mydayboard.core.domain.task.TaskCategory
import io.github.ddiling.mydayboard.core.domain.task.TaskFormError
import io.github.ddiling.mydayboard.feature.tasks.R

/** Stateless: everything shown comes from [uiState] and [categories], everything the user does goes to [actions]. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TaskFormScreen(
    uiState: TaskFormUiState,
    categories: List<TaskCategory>,
    actions: TaskFormActions,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = actions.onClose)
    val title = if (uiState.isEditing) R.string.tasks_form_title_edit else R.string.tasks_form_title_new
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(title)) },
                navigationIcon = {
                    IconButton(onClick = actions.onClose, modifier = Modifier.testTag("tasks_form_close")) {
                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.tasks_form_close))
                    }
                },
                actions = {
                    TextButton(
                        onClick = actions.onSave,
                        enabled = uiState.canSave,
                        modifier = Modifier.testTag("tasks_form_save"),
                    ) { Text(stringResource(R.string.tasks_form_save)) }
                },
            )
        },
    ) { padding ->
        val contentModifier = Modifier.fillMaxSize().padding(padding)
        when (uiState.load) {
            FormLoad.Loading -> Box(contentModifier)
            FormLoad.NotFound -> NotFound(contentModifier)
            FormLoad.Ready -> Form(uiState, categories, actions, contentModifier)
        }
    }
}

@Composable
private fun NotFound(modifier: Modifier = Modifier) {
    Box(modifier, Alignment.Center) { Text(stringResource(R.string.tasks_form_not_found)) }
}

/**
 * The name error waits for the first edit, so an untouched empty form does not scold; a name that is already
 * non-empty (an edited task) shows its error at once.
 */
@Composable
private fun Form(
    uiState: TaskFormUiState,
    categories: List<TaskCategory>,
    actions: TaskFormActions,
    modifier: Modifier = Modifier,
) {
    var nameTouched by rememberSaveable { mutableStateOf(false) }
    val nameError = uiState.errors.firstOrNull { it is TaskFormError.BlankName || it is TaskFormError.NameTooLong }
    Column(
        modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 16.dp),
        Arrangement.spacedBy(24.dp),
    ) {
        DashedTextField(
            label = stringResource(R.string.tasks_form_name),
            value = uiState.name,
            onValueChange = {
                nameTouched = true
                actions.onNameChange(it)
            },
            error = nameError.takeIf { nameTouched || uiState.name.isNotEmpty() }?.message(),
            modifier = Modifier.testTag("tasks_form_name"),
        )
        CategoryField(uiState.categoryId, categories, actions.onCategoryChange)
        RepeatField(uiState.repeatType, actions.onRepeatTypeChange)
        RepeatSections(uiState, actions)
    }
}
