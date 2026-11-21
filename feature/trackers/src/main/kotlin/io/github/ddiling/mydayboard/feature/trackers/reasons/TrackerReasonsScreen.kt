package io.github.ddiling.mydayboard.feature.trackers.reasons

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.ddiling.mydayboard.feature.trackers.R

/**
 * Placeholder that only lists the reasons as plain text, so the route and its state can be reviewed and wired
 * up first; the real screen replaces it in the next change.
 */
@Composable
internal fun TrackerReasonsRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TrackerReasonsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    Column(modifier.padding(20.dp)) {
        (uiState as? TrackerReasonsUiState.Content)?.reasons?.forEach { Text(it.text) }
        Button(onClick = onBack) { Text(stringResource(R.string.trackers_editor_back)) }
    }
}
