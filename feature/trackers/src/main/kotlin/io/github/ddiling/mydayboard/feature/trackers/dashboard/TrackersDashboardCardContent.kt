package io.github.ddiling.mydayboard.feature.trackers.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.ddiling.mydayboard.core.dashboard.DashboardHost
import io.github.ddiling.mydayboard.feature.trackers.R
import io.github.ddiling.mydayboard.feature.trackers.detail.UndoSnackbar
import io.github.ddiling.mydayboard.feature.trackers.list.asLastEventText
import io.github.ddiling.mydayboard.feature.trackers.navigation.TrackerDetailDestination
import io.github.ddiling.mydayboard.feature.trackers.navigation.TrackerEditorDestination

private val ActionButtonMinHeight = 48.dp

/**
 * Stateless so tests and previews need no ViewModel. The rows are a plain [Column], not a lazy list: the
 * dashboard already scrolls its cards in a `LazyColumn`, and a nested list scrolling the same way crashes.
 * Navigation and the undo snackbar go through [host], because the dashboard owns the screen they live on.
 */
@Composable
internal fun TrackersDashboardCardContent(
    uiState: TrackersDashboardCardUiState,
    actions: TrackersDashboardCardActions,
    host: DashboardHost,
    modifier: Modifier = Modifier,
) {
    val content = uiState as? TrackersDashboardCardUiState.Content
    UndoSnackbar(content?.pendingUndo, host.snackbarHostState, actions.onUndo, actions.onUndoDismissed)
    if (content == null) return
    Card(modifier.testTag("trackers_card")) {
        if (content.rows.isEmpty()) {
            EmptyState { host.navigate(TrackerEditorDestination()) }
        } else {
            Column(Modifier.padding(vertical = 8.dp)) {
                content.rows.forEach { row ->
                    TrackerRow(row, actions, onClick = { host.navigate(TrackerDetailDestination(row.id)) })
                }
            }
        }
    }
}

@Composable
private fun EmptyState(onAddClick: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(stringResource(R.string.trackers_empty_hint))
        Button(onClick = onAddClick, modifier = Modifier.testTag("trackers_card_add")) {
            Text(stringResource(R.string.trackers_add))
        }
    }
}

@Composable
private fun TrackerRow(
    row: TrackersDashboardRow,
    actions: TrackersDashboardCardActions,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .testTag("trackers_card_row_${row.id}")
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(row.name, style = MaterialTheme.typography.titleMedium)
            Text(row.detailText(), style = MaterialTheme.typography.bodyMedium)
        }
        when (row) {
            is TrackersDashboardRow.Counter -> {
                val description = stringResource(R.string.trackers_card_plus_one_description, row.name)
                ActionButton(
                    label = stringResource(R.string.trackers_detail_plus_one),
                    description = description,
                    onClick = { actions.onPlusOne(row.id) },
                    modifier = Modifier.testTag("trackers_card_plus_one_${row.id}"),
                )
            }

            is TrackersDashboardRow.DaysSince -> {
                val description = stringResource(R.string.trackers_card_reset_description, row.name)
                ActionButton(
                    label = stringResource(R.string.trackers_detail_reset),
                    description = description,
                    onClick = { actions.onReset(row.id) },
                    modifier = Modifier.testTag("trackers_card_reset_${row.id}"),
                )
            }
        }
    }
}

@Composable
private fun ActionButton(
    label: String,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier,
) {
    Button(
        onClick = onClick,
        modifier = modifier.heightIn(min = ActionButtonMinHeight).semantics { contentDescription = description },
    ) { Text(label) }
}

@Composable
private fun TrackersDashboardRow.detailText(): String =
    when (this) {
        is TrackersDashboardRow.Counter -> {
            "${stringResource(R.string.trackers_today_count, todayCount)} · ${sinceLast.asLastEventText()}"
        }

        is TrackersDashboardRow.DaysSince -> {
            pluralStringResource(R.plurals.trackers_days, days, days)
        }
    }
