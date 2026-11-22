package io.github.ddiling.mydayboard.feature.tasks.detail

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.ddiling.mydayboard.feature.tasks.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TaskDetailRoute(
    onEdit: () -> Unit,
    onBack: () -> Unit,
    viewModel: TaskDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(state?.name.orEmpty()) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.Close, stringResource(R.string.tasks_detail_back)) }
                },
            )
        },
    ) { padding ->
        val s = state ?: return@Scaffold
        Column(Modifier.padding(padding).padding(horizontal = 20.dp), verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
            Text(s.repeat, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(stringResource(R.string.tasks_detail_next_due, s.nextDue))
            Text(stringResource(R.string.tasks_detail_streak, s.streak))
            Text(stringResource(R.string.tasks_detail_last_days, s.doneOfPlanned, s.windowDays))
            TextButton(onClick = viewModel::markDone) { Text(stringResource(R.string.tasks_detail_mark_done)) }
            TextButton(onClick = viewModel::untick) { Text(stringResource(R.string.tasks_detail_untick)) }
            TextButton(onClick = onEdit) { Text(stringResource(R.string.tasks_detail_edit)) }
            TextButton(onClick = { viewModel.archive(onBack) }) { Text(stringResource(R.string.tasks_detail_archive)) }
        }
    }
}
