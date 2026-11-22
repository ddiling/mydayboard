package io.github.ddiling.mydayboard.feature.tasks.list

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.ddiling.mydayboard.core.domain.task.groupedByCategory
import io.github.ddiling.mydayboard.feature.tasks.R
import io.github.ddiling.mydayboard.feature.tasks.ui.TaskRowView
import io.github.ddiling.mydayboard.feature.tasks.ui.TasksOverviewViewModel
import io.github.ddiling.mydayboard.feature.tasks.ui.rememberSwipeDone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TasksListRoute(
    onTaskClick: (String) -> Unit,
    onAddClick: () -> Unit,
    onBack: () -> Unit,
    viewModel: TasksOverviewViewModel = hiltViewModel(),
) {
    val overview by viewModel.overview.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val swipeDone = rememberSwipeDone(viewModel, snackbar)
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.tasks_list_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.Close, stringResource(R.string.tasks_detail_back)) }
                },
                actions = {
                    IconButton(onClick = onAddClick) { Icon(Icons.Default.Add, stringResource(R.string.tasks_add)) }
                },
            )
        },
    ) { padding ->
        val groups = overview?.groupedByCategory().orEmpty()
        LazyColumn(Modifier.padding(padding)) {
            groups.forEach { group ->
                item(key = "header_${group.category?.id}") {
                    Text(
                        (group.category?.name ?: stringResource(R.string.tasks_uncategorized)).uppercase(),
                        Modifier.padding(start = 20.dp, top = 16.dp, bottom = 4.dp),
                        MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                items(group.rows, key = { it.task.id }) { row ->
                    TaskRowView(row, onSwipeDone = { swipeDone(row.task.id) }, onClick = { onTaskClick(row.task.id) })
                }
            }
        }
    }
}
