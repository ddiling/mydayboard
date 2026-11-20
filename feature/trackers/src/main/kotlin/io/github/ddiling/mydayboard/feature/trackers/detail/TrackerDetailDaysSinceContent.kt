package io.github.ddiling.mydayboard.feature.trackers.detail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.ddiling.mydayboard.feature.trackers.R

/** The days are always written out as text, so the streak never relies on colour or position alone. */
@Composable
internal fun DaysSinceContent(
    state: TrackerDetailUiState.DaysSince,
    onStreakClick: (eventId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier, contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 88.dp)) {
        item { DaysSinceHeader(state) }
        item { SectionTitle(R.string.trackers_detail_earlier) }
        if (state.brokenStreaks.isEmpty()) item { Text(stringResource(R.string.trackers_detail_earlier_none)) }
        items(state.brokenStreaks) { streak ->
            // Stacked, not side by side: a long date range and the days would squeeze each other at large font sizes.
            // A tap opens the reset that ended the streak for editing.
            Column(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .clickable { onStreakClick(streak.endedByEventId) }
                    .testTag("trackers_detail_entry_${streak.endedByEventId}")
                    .padding(vertical = 8.dp),
            ) {
                Text(stringResource(R.string.trackers_detail_range, streak.start.formatted(), streak.end.formatted()))
                Text(streak.days.asDaysText(), style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun DaysSinceHeader(state: TrackerDetailUiState.DaysSince) {
    Column(Modifier.padding(vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(state.currentDays.asDaysText(), style = MaterialTheme.typography.headlineLarge)
        Text(stringResource(R.string.trackers_detail_since, state.currentStart.formatted()))
        Text(stringResource(R.string.trackers_detail_longest, state.longestDays.asDaysText()))
    }
}

/** Plural rules take an Int; a streak longer than Int.MAX_VALUE days is not realistic, so it is capped. */
@Composable
private fun Long.asDaysText(): String {
    val count = coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
    return pluralStringResource(R.plurals.trackers_days, count, count)
}
