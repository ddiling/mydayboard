package io.github.ddiling.mydayboard.feature.tasks.categories

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.unit.dp
import io.github.ddiling.mydayboard.feature.tasks.R
import io.github.ddiling.mydayboard.feature.tasks.detail.creditText
import io.github.ddiling.mydayboard.feature.tasks.detail.deviceLocale
import io.github.ddiling.mydayboard.feature.tasks.list.FolderIcon

private const val HERO_FORMAT = "%.1f"

/**
 * The numbers first (so a screen reader says the hero first), then the tasks, then the archived ones. Archived
 * tasks are grey and cannot be opened: they still count in the numbers, which is why they are listed.
 */
@Composable
internal fun CategoryStatsBody(
    content: CategoryStatsUiState.Content,
    onTaskClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier.padding(horizontal = 20.dp).testTag("tasks_catstats_content")) {
        item(key = "hero") { Hero(content.perDay) }
        item(key = "stats") { Stats(content) }
        if (content.tasks.isNotEmpty()) {
            item(key = "tasks-title") {
                SectionTitle(
                    Icons.AutoMirrored.Filled.List,
                    R.string.tasks_catstats_tasks_title,
                    "tasks_catstats_tasks_title",
                )
            }
            items(content.tasks, key = { it.id }) { TaskLineRow(it, onClick = { onTaskClick(it.id) }) }
        }
        if (content.archivedLines.isNotEmpty()) {
            item(key = "archived-title") {
                SectionTitle(FolderIcon, R.string.tasks_catstats_archived_title, "tasks_catstats_archived_title")
            }
            items(content.archivedLines, key = { it.id }) { TaskLineRow(it, onClick = null) }
        }
    }
}

@Composable
private fun Hero(perDay: Double) {
    val value = String.format(LocalResources.current.deviceLocale(), HERO_FORMAT, perDay)
    Column(Modifier.fillMaxWidth().padding(bottom = 16.dp).semantics(mergeDescendants = true) { heading() }) {
        Text(
            value,
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Light,
            modifier = Modifier.testTag("tasks_catstats_hero"),
        )
        Text(
            stringResource(R.string.tasks_catstats_hero_unit),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.testTag("tasks_catstats_hero_unit"),
        )
    }
}

/** Three cells; at a large font the labels wrap and the row grows instead of cutting text. */
@Composable
private fun Stats(content: CategoryStatsUiState.Content) {
    val resources = LocalResources.current
    val of =
        resources.getString(
            R.string.tasks_detail_stat_of,
            creditText(resources, content.doneOfPlanned),
            content.plannedDays,
        )
    Row(
        Modifier.fillMaxWidth().padding(bottom = 16.dp).testTag("tasks_catstats_stats"),
        Arrangement.spacedBy(12.dp),
    ) {
        StatCell(content.tasks.size.toString(), stringResource(R.string.tasks_catstats_stat_tasks), Modifier.weight(1f))
        StatCell(of, stringResource(R.string.tasks_detail_stat_recent), Modifier.weight(1f))
        StatCell(content.skipped.toString(), stringResource(R.string.tasks_detail_stat_skipped), Modifier.weight(1f))
    }
}

@Composable
private fun StatCell(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier.heightIn(min = 48.dp).semantics(mergeDescendants = true) {}) {
        Text(value, style = MaterialTheme.typography.titleLarge)
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** A small grey uppercase title with a 16 dp icon; a heading so screen readers can jump to it. */
@Composable
private fun SectionTitle(
    icon: ImageVector,
    text: Int,
    tag: String,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .semantics(mergeDescendants = true) { heading() }
            .testTag(tag),
        Arrangement.spacedBy(8.dp),
        Alignment.CenterVertically,
    ) {
        Icon(icon, null, Modifier.size(16.dp), MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            stringResource(text).uppercase(Locale.current.platformLocale),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** One task: its name and "skipped 2 times". With an [onClick] the row opens the task and shows an arrow. */
@Composable
private fun TaskLineRow(
    line: TaskLine,
    onClick: (() -> Unit)?,
) {
    val grey = MaterialTheme.colorScheme.onSurfaceVariant
    val clickable = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .then(clickable)
            .padding(vertical = 6.dp)
            .semantics(mergeDescendants = true) {}
            .testTag("tasks_catstats_row_${line.id}"),
        Arrangement.spacedBy(12.dp),
        Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                line.name,
                style = MaterialTheme.typography.bodyMedium,
                color =
                    if (onClick ==
                        null
                    ) {
                        grey
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
            )
            Text(
                pluralStringResource(R.plurals.tasks_catstats_skipped, line.skipped, line.skipped),
                style = MaterialTheme.typography.bodySmall,
                color = grey,
            )
        }
        if (onClick != null) Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = grey)
    }
}
