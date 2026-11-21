package io.github.ddiling.mydayboard.feature.trackers.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.ddiling.mydayboard.core.dashboard.DashboardHost
import io.github.ddiling.mydayboard.core.navigation.Destination
import io.github.ddiling.mydayboard.feature.trackers.R
import io.github.ddiling.mydayboard.feature.trackers.detail.UndoSnackbar
import io.github.ddiling.mydayboard.feature.trackers.navigation.TrackerDetailDestination
import io.github.ddiling.mydayboard.feature.trackers.navigation.TrackerEditorDestination
import io.github.ddiling.mydayboard.feature.trackers.navigation.TrackersListDestination

private const val COLUMNS = 2

/**
 * Stateless so tests and previews need no ViewModel. The grid is plain rows, not a lazy list: the dashboard
 * already scrolls its cards in a `LazyColumn`, and a nested list scrolling the same way crashes. Navigation and
 * the undo snackbar go through [host], because the dashboard owns the screen they live on. Every navigation
 * dismisses the pending undo first: the screen it opens has its own snackbar, so the offer must not outlive
 * the card. The card adds no horizontal padding and no background; the dashboard sets its own margins.
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
    val open: (Destination) -> Unit = {
        actions.onUndoDismissed()
        host.navigate(it)
    }
    Column(modifier.testTag("trackers_card")) {
        TitleRow(
            totalCount = content.totalCount,
            onOpenList = { open(TrackersListDestination) },
            onAdd = { open(TrackerEditorDestination()) },
        )
        val places = content.rows + List(MAX_PLACES - content.rows.size) { null }
        places.chunked(COLUMNS).forEachIndexed { rowIndex, rowPlaces ->
            // The row height follows its taller tile, so a large font cannot cut the vertical line short.
            Row(Modifier.height(IntrinsicSize.Min)) {
                rowPlaces.forEachIndexed { columnIndex, row ->
                    val tile =
                        Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .dashedSeparators(isLeft = columnIndex == 0, isTopRow = rowIndex == 0)
                    if (row == null) {
                        val slotTag = "empty_slot_${rowIndex * COLUMNS + columnIndex}"
                        EmptyTile(isLeft = columnIndex == 0, tile.testTag(slotTag)) { open(TrackerEditorDestination()) }
                    } else {
                        val tileTag = "trackers_card_tile_${row.id}"
                        TrackerTile(row, isLeft = columnIndex == 0, actions, tile.testTag(tileTag)) {
                            open(TrackerDetailDestination(row.id))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TitleRow(
    totalCount: Int,
    onOpenList: () -> Unit,
    onAdd: () -> Unit,
) {
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Row(
            Modifier
                .weight(1f)
                .heightIn(min = 48.dp)
                .clickable(onClick = onOpenList)
                .testTag("open_trackers"),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(PulseIcon, contentDescription = null, Modifier.size(16.dp), muted)
            Text(
                stringResource(R.string.trackers_dashboard_title).uppercase(),
                Modifier.padding(start = 6.dp),
                muted,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.8.sp,
            )
            Text(totalCount.toString(), Modifier.padding(start = 6.dp), fontSize = 13.sp)
        }
        IconButton(onClick = onAdd, modifier = Modifier.testTag("add_tracker")) {
            val description = stringResource(R.string.trackers_dashboard_add_content_description)
            Icon(PlusIcon, description, Modifier.size(24.dp), muted)
        }
    }
}
