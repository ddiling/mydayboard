package io.github.ddiling.mydayboard.feature.tasks.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.ddiling.mydayboard.core.designsystem.theme.TaskColors
import io.github.ddiling.mydayboard.feature.tasks.R

private const val STATS_PER_ROW = 3

/**
 * The details, top to bottom: the status word first (so a screen reader says it first), the stats, then the
 * sections. Everything scrolls but the bottom action, which stays within reach.
 */
@Composable
internal fun TaskDetailBody(
    content: TaskDetailUiState.Content,
    actions: TaskDetailActions,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        LazyColumn(
            Modifier.weight(1f).padding(horizontal = 20.dp).testTag("tasks_detail_content"),
        ) {
            item(key = "hero") { Hero(content) }
            item(key = "stats") { StatsGrid(content.stats) }
            if (content.chips.isNotEmpty()) {
                item(key = "chips") {
                    TimeChips(content.chips, actions.onChipClick, Modifier.testTag("tasks_detail_chips"))
                }
            }
            if (content.history.isNotEmpty()) {
                item(key = "history-title") { HistoryTitle() }
                items(content.history, key = { it.day.toString() }) { row ->
                    HistoryRowLine(row, content.today) { actions.onHistoryRowClick(row.day) }
                }
            }
        }
        BottomAction(content, actions.onBigButton)
    }
}

@Composable
private fun Hero(content: TaskDetailUiState.Content) {
    val resources = LocalResources.current
    val hero = content.hero
    val good = hero == Hero.DoneToday || hero == Hero.Finished(reached = true)
    val bad = hero is Hero.LateSince || hero == Hero.Finished(reached = false)
    val color =
        when {
            good -> TaskColors.done
            bad -> TaskColors.late
            else -> MaterialTheme.colorScheme.onSurface
        }
    Column(Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
        Text(
            heroText(resources, hero, content.today),
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Light,
            color = color,
            modifier = Modifier.semantics { heading() }.testTag("tasks_detail_hero"),
        )
        val subtitle = subtitleText(resources, content.task, content.categoryName)
        if (subtitle.isNotEmpty()) Grey(subtitle, Modifier.testTag("tasks_detail_subtitle"))
        if (hero is Hero.LateSince) {
            Grey(stringResource(R.string.tasks_detail_late_note), Modifier.testTag("tasks_detail_late_note"))
        }
    }
}

@Composable
private fun Grey(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    )
}

/** Three cells to a row; at a large font the labels wrap and the rows grow instead of cutting text. */
@Composable
private fun StatsGrid(stats: DetailStats) {
    val cells = statCells(LocalResources.current, stats)
    Column(Modifier.fillMaxWidth().padding(bottom = 16.dp).testTag("tasks_detail_stats"), Arrangement.spacedBy(12.dp)) {
        cells.chunked(STATS_PER_ROW).forEach { rowCells ->
            Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(12.dp)) {
                rowCells.forEach { cell -> StatCellView(cell, Modifier.weight(1f)) }
                repeat(STATS_PER_ROW - rowCells.size) { Column(Modifier.weight(1f)) {} }
            }
        }
    }
}

@Composable
private fun StatCellView(
    cell: StatCell,
    modifier: Modifier = Modifier,
) {
    Column(modifier.heightIn(min = 48.dp).semantics(mergeDescendants = true) {}) {
        Text(cell.value, style = MaterialTheme.typography.titleLarge)
        Grey(cell.label)
    }
}
