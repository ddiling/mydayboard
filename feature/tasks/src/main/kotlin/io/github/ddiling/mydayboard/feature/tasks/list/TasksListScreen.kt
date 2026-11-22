package io.github.ddiling.mydayboard.feature.tasks.list

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.ddiling.mydayboard.core.designsystem.component.PrimaryButton
import io.github.ddiling.mydayboard.core.designsystem.theme.TaskColors
import io.github.ddiling.mydayboard.feature.tasks.R

/** Stateless: everything shown comes from [uiState]. The late count sits in the top bar, to find late tasks. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TasksListScreen(
    uiState: TasksListUiState,
    actions: TasksListActions,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = actions.onBack)
    val content = uiState as? TasksListUiState.Content
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.tasks_list_title)) },
                navigationIcon = {
                    IconButton(onClick = actions.onBack, modifier = Modifier.testTag("tasks_list_back")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.tasks_list_back))
                    }
                },
                actions = {
                    if (content != null && content.lateCount > 0) {
                        Text(
                            pluralStringResource(R.plurals.tasks_list_late, content.lateCount, content.lateCount),
                            color = TaskColors.late,
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.testTag("tasks_list_late"),
                        )
                    }
                    IconButton(onClick = actions.onAddTask, modifier = Modifier.testTag("tasks_list_add")) {
                        Icon(Icons.Default.Add, stringResource(R.string.tasks_list_add))
                    }
                },
            )
        },
    ) { padding ->
        val body = Modifier.fillMaxSize().padding(padding)
        when (uiState) {
            TasksListUiState.Loading -> Loading(body)
            TasksListUiState.Empty -> EmptyState(actions.onAddTask, body)
            is TasksListUiState.Content -> TasksListContent(uiState, actions, body)
        }
    }
}

@Composable
private fun Loading(modifier: Modifier = Modifier) {
    Box(modifier, Alignment.Center) { CircularProgressIndicator(Modifier.testTag("tasks_list_loading")) }
}

@Composable
private fun EmptyState(
    onAddTask: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier.padding(horizontal = 20.dp),
        Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        Alignment.CenterHorizontally,
    ) {
        Text(
            stringResource(R.string.tasks_list_empty_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.testTag("tasks_list_empty_hint"),
        )
        PrimaryButton(
            stringResource(R.string.tasks_list_add),
            onAddTask,
            Modifier.testTag("tasks_list_empty_add"),
        )
    }
}
