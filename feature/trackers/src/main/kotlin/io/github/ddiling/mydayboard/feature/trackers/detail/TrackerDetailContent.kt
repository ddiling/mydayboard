package io.github.ddiling.mydayboard.feature.trackers.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.ddiling.mydayboard.core.domain.tracker.Trend
import io.github.ddiling.mydayboard.feature.trackers.R
import io.github.ddiling.mydayboard.feature.trackers.list.asLastEventText
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
internal fun CounterContent(
    counter: TrackerDetailUiState.Counter,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier, contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 88.dp)) {
        item { Header(counter) }
        item { SectionTitle(R.string.trackers_detail_last_days) }
        // The newest day first, like the history below.
        items(counter.lastDays.asReversed()) { day ->
            LabeledRow(day.date.formatted(), day.count.toString())
        }
        item { SectionTitle(R.string.trackers_detail_history) }
        if (counter.history.isEmpty()) item { Text(stringResource(R.string.trackers_no_events)) }
        items(counter.history) { day -> HistoryDayBlock(day) }
    }
}

@Composable
private fun Header(counter: TrackerDetailUiState.Counter) {
    Column(Modifier.padding(vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            stringResource(R.string.trackers_today_count, counter.todayCount),
            style = MaterialTheme.typography.headlineLarge,
        )
        Text(counter.sinceLast.asLastEventText())
        TrendLabel(counter.trend)
    }
}

/** The icon repeats what the text says, so the trend never relies on colour or shape alone. */
@Composable
private fun TrendLabel(trend: Trend) {
    val (iconRes, rotation, textRes) =
        when (trend) {
            Trend.UP -> Triple(R.drawable.ic_trend_arrow, 0f, R.string.trackers_trend_up)
            Trend.DOWN -> Triple(R.drawable.ic_trend_arrow, 180f, R.string.trackers_trend_down)
            Trend.FLAT -> Triple(R.drawable.ic_trend_arrow, 90f, R.string.trackers_trend_flat)
            Trend.NOT_ENOUGH_DATA -> Triple(R.drawable.ic_trend_unknown, 0f, R.string.trackers_trend_not_enough_data)
        }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(painterResource(iconRes), contentDescription = null, modifier = Modifier.rotate(rotation))
        Text(stringResource(textRes))
    }
}

@Composable
internal fun SectionTitle(titleRes: Int) {
    Text(
        stringResource(titleRes),
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(top = 24.dp, bottom = 8.dp),
    )
}

@Composable
internal fun LabeledRow(
    label: String,
    value: String,
) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), Arrangement.SpaceBetween) {
        Text(label)
        Text(value)
    }
}

@Composable
private fun HistoryDayBlock(day: HistoryDay) {
    Column(Modifier.padding(vertical = 8.dp)) {
        Text(day.date.formatted(), style = MaterialTheme.typography.labelLarge)
        day.times.forEach { time -> Text(time.formatted()) }
    }
}

internal fun LocalDate.formatted(): String = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).format(this)

private fun LocalTime.formatted(): String = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).format(this)
