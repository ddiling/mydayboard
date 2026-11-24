package io.github.ddiling.mydayboard.feature.tasks.detail

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.ddiling.mydayboard.core.designsystem.component.PrimaryButton
import io.github.ddiling.mydayboard.feature.tasks.R

/** Stateless: everything shown comes from [uiState]. The name is the title and the pencil opens the edit form. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TaskDetailScreen(
    uiState: TaskDetailUiState,
    actions: TaskDetailActions,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = actions.onBack)
    val content = uiState as? TaskDetailUiState.Content
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(content?.task?.name.orEmpty(), modifier = Modifier.testTag("tasks_detail_title")) },
                navigationIcon = {
                    IconButton(onClick = actions.onBack, modifier = Modifier.testTag("tasks_detail_back")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.tasks_detail_back))
                    }
                },
                actions = {
                    if (content != null) {
                        IconButton(onClick = actions.onEdit, modifier = Modifier.testTag("tasks_detail_edit")) {
                            Icon(Icons.Default.Edit, stringResource(R.string.tasks_detail_edit))
                        }
                    }
                },
            )
        },
    ) { padding ->
        val body = Modifier.fillMaxSize().padding(padding)
        when (uiState) {
            TaskDetailUiState.Loading -> Loading(body)
            TaskDetailUiState.NotFound -> NotFound(actions.onBack, body)
            is TaskDetailUiState.Content -> TaskDetailBody(uiState, body)
        }
    }
}

@Composable
private fun Loading(modifier: Modifier = Modifier) {
    Box(modifier, Alignment.Center) { CircularProgressIndicator(Modifier.testTag("tasks_detail_loading")) }
}

@Composable
private fun NotFound(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier.padding(horizontal = 20.dp),
        Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        Alignment.CenterHorizontally,
    ) {
        Text(
            stringResource(R.string.tasks_detail_not_found_title),
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() }.testTag("tasks_detail_not_found"),
        )
        Text(
            stringResource(R.string.tasks_detail_not_found_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        PrimaryButton(
            stringResource(R.string.tasks_detail_not_found_back),
            onBack,
            Modifier.testTag("tasks_detail_not_found_back"),
        )
    }
}
