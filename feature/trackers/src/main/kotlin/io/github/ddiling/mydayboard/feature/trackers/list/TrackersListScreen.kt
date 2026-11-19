package io.github.ddiling.mydayboard.feature.trackers.list

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.ddiling.mydayboard.core.designsystem.preview.PreviewSurface
import io.github.ddiling.mydayboard.feature.trackers.R
import io.github.ddiling.mydayboard.feature.trackers.format.DurationParts
import io.github.ddiling.mydayboard.feature.trackers.format.asText
import io.github.ddiling.mydayboard.feature.trackers.format.toParts
import java.time.Duration

/** Hilt provides the ViewModel; the callbacks keep navigation out of the feature's screens. */
@Composable
internal fun TrackersListRoute(
    onTrackerClick: (String) -> Unit,
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TrackersListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    TrackersListScreen(uiState, onTrackerClick, onAddClick, modifier)
}

/** The add button is a FAB once there are rows and part of the empty state before, so it never shows twice. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TrackersListScreen(
    uiState: TrackersListUiState,
    onTrackerClick: (String) -> Unit,
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text(stringResource(R.string.trackers_title)) }) },
        floatingActionButton = {
            if (uiState is TrackersListUiState.Content) {
                ExtendedFloatingActionButton(onClick = onAddClick) { Text(stringResource(R.string.trackers_add)) }
            }
        },
    ) { padding ->
        val contentModifier = Modifier.fillMaxSize().padding(padding)
        when (uiState) {
            TrackersListUiState.Loading -> {
                Box(contentModifier, contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            }

            TrackersListUiState.Empty -> {
                EmptyState(onAddClick, contentModifier)
            }

            is TrackersListUiState.Content -> {
                LazyColumn(contentModifier) {
                    items(uiState.items, key = { it.id }) { item ->
                        TrackerRow(item, Modifier.clickable { onTrackerClick(item.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyState(
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(stringResource(R.string.trackers_empty_hint))
        Button(onClick = onAddClick) { Text(stringResource(R.string.trackers_add)) }
    }
}

@Composable
private fun TrackerRow(
    item: TrackerListItem,
    modifier: Modifier = Modifier,
) {
    ListItem(
        headlineContent = { Text(item.name) },
        supportingContent = { if (item is TrackerListItem.Counter) Text(item.sinceLast.asLastEventText()) },
        trailingContent = { Text(item.trailingText()) },
        modifier = modifier,
    )
}

@Composable
private fun TrackerListItem.trailingText(): String =
    when (this) {
        is TrackerListItem.Counter -> stringResource(R.string.trackers_today_count, todayCount)
        is TrackerListItem.DaysSince -> pluralStringResource(R.plurals.trackers_days, days, days)
    }

@Composable
private fun Duration?.asLastEventText(): String =
    when {
        this == null -> stringResource(R.string.trackers_no_events)
        toParts() == DurationParts.JustNow -> asText()
        else -> stringResource(R.string.trackers_last_ago, asText())
    }

private val previewItems =
    listOf(
        TrackerListItem.Counter("1", "Water", todayCount = 3, sinceLast = Duration.ofMinutes(200)),
        TrackerListItem.Counter("2", "Stretching", todayCount = 0, sinceLast = null),
        TrackerListItem.DaysSince("3", "No sugar", days = 12),
    )

@PreviewLightDark
@Composable
private fun TrackersListLoadingPreview() {
    PreviewSurface { TrackersListScreen(TrackersListUiState.Loading, {}, {}) }
}

@PreviewLightDark
@Composable
private fun TrackersListEmptyPreview() {
    PreviewSurface { TrackersListScreen(TrackersListUiState.Empty, {}, {}) }
}

@PreviewLightDark
@Composable
private fun TrackersListContentPreview() {
    PreviewSurface { TrackersListScreen(TrackersListUiState.Content(previewItems), {}, {}) }
}

@Preview(name = "Large font light", fontScale = 2f)
@Preview(name = "Large font dark", fontScale = 2f, uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun TrackersListLargeFontPreview() {
    PreviewSurface { TrackersListScreen(TrackersListUiState.Content(previewItems), {}, {}) }
}
