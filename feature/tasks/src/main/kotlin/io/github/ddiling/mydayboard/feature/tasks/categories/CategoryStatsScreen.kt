package io.github.ddiling.mydayboard.feature.tasks.categories

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
import io.github.ddiling.mydayboard.feature.tasks.R

/**
 * Stateless: everything shown comes from [uiState] and [dialog]. The name is the title and the pencil opens the
 * category dialog, which floats above while it is open.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CategoryStatsScreen(
    uiState: CategoryStatsUiState,
    actions: CategoryStatsActions,
    modifier: Modifier = Modifier,
    dialog: CategoryDialogState = CategoryDialogState.Closed,
    dialogActions: CategoryDialogActions = CategoryDialogActions(),
) {
    BackHandler(onBack = actions.onBack)
    if (dialog is CategoryDialogState.Editing) CategoryDialog(dialog, dialogActions)
    val title =
        when (uiState) {
            is CategoryStatsUiState.Content -> uiState.categoryName
            is CategoryStatsUiState.Empty -> uiState.categoryName
            CategoryStatsUiState.Loading, CategoryStatsUiState.NotFound -> ""
        }
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(title, modifier = Modifier.testTag("tasks_catstats_title")) },
                navigationIcon = {
                    IconButton(onClick = actions.onBack, modifier = Modifier.testTag("tasks_catstats_back")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.tasks_catstats_back))
                    }
                },
                actions = {
                    if (title.isNotEmpty()) {
                        IconButton(onClick = actions.onEdit, modifier = Modifier.testTag("tasks_catstats_edit")) {
                            Icon(Icons.Default.Edit, stringResource(R.string.tasks_catstats_edit))
                        }
                    }
                },
            )
        },
    ) { padding ->
        val body = Modifier.fillMaxSize().padding(padding)
        when (uiState) {
            CategoryStatsUiState.Loading -> {
                Box(body, Alignment.Center) { CircularProgressIndicator(Modifier.testTag("tasks_catstats_loading")) }
            }

            CategoryStatsUiState.NotFound -> {
                Message(R.string.tasks_catstats_not_found_title, R.string.tasks_catstats_not_found_hint, body)
            }

            is CategoryStatsUiState.Empty -> {
                Message(
                    R.string.tasks_catstats_empty_title,
                    R.string.tasks_catstats_empty_hint,
                    body,
                    uiState.categoryName,
                )
            }

            is CategoryStatsUiState.Content -> {
                CategoryStatsBody(uiState, actions.onTaskClick, body)
            }
        }
    }
}

@Composable
private fun Message(
    title: Int,
    hint: Int,
    modifier: Modifier = Modifier,
    name: String = "",
) {
    Column(
        modifier.padding(horizontal = 20.dp).testTag("tasks_catstats_message"),
        Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        Alignment.CenterHorizontally,
    ) {
        Text(
            stringResource(title, name),
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            stringResource(hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
