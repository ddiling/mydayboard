package io.github.ddiling.mydayboard.feature.tasks.list

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.unit.dp
import io.github.ddiling.mydayboard.core.designsystem.theme.DashedSeparatorColor
import io.github.ddiling.mydayboard.core.domain.task.RowState
import io.github.ddiling.mydayboard.core.domain.task.TaskGroup
import io.github.ddiling.mydayboard.feature.tasks.R
import io.github.ddiling.mydayboard.feature.tasks.common.SwipeToDoneRow
import io.github.ddiling.mydayboard.feature.tasks.common.TaskRowLine

private val DashLength = 4.dp

/** The groups one under another: a small title, then its rows, with a dashed line between groups. */
@Composable
internal fun TasksListContent(
    content: TasksListUiState.Content,
    actions: TasksListActions,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier.padding(horizontal = 20.dp).testTag("tasks_list_content")) {
        content.groups.forEachIndexed { index, group ->
            if (index > 0) item(key = "divider-${group.category?.id}") { DashedDivider() }
            item(key = "title-${group.category?.id}") { GroupTitle(group, actions.onCategoryStats) }
            items(group.rows, key = { it.task.id }) { row ->
                val actionable = row.state == RowState.PENDING || row.state == RowState.LATE
                SwipeToDoneRow(onDone = { actions.onSwipeDone(row.task.id) }, enabled = actionable) {
                    TaskRowLine(row, content.today, onClick = { actions.onTaskClick(row.task.id) })
                }
            }
        }
        if (content.archivedCount > 0) {
            // No divider when no group is shown above it: a line with nothing before it would look like a stray rule.
            if (content.groups.isNotEmpty()) {
                item(key = "archived-divider") { DashedDivider(Modifier.testTag("tasks_list_archived_divider")) }
            }
            item(key = "archived") { ArchivedRow(content.archivedCount, actions.onArchiveClick) }
        }
    }
}

/**
 * A heading for screen readers, so they can jump from category to category. A real category also has a chart
 * button that opens its stats; "No category" has none, because it is not a category that can be opened. The button
 * sits beside the heading, not inside it, so it stays a separate button for screen readers.
 */
@Composable
private fun GroupTitle(
    group: TaskGroup,
    onCategoryStats: (String) -> Unit,
) {
    val category = group.category
    val name = category?.name ?: stringResource(R.string.tasks_form_category_none)
    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
        Row(
            Modifier
                .weight(1f)
                .heightIn(min = 48.dp)
                .semantics(mergeDescendants = true) { heading() }
                .testTag("tasks_list_group_${category?.id}"),
            Arrangement.spacedBy(8.dp),
            Alignment.CenterVertically,
        ) {
            Icon(
                FolderIcon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                name.uppercase(Locale.current.platformLocale),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (category != null) {
            IconButton(
                onClick = { onCategoryStats(category.id) },
                modifier = Modifier.testTag("tasks_list_stats_${category.id}"),
            ) {
                Icon(
                    ChartIcon,
                    stringResource(R.string.tasks_list_category_stats, category.name),
                    Modifier.size(16.dp),
                    MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun DashedDivider(modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .height(1.dp)
            .drawBehind {
                val dashes = PathEffect.dashPathEffect(floatArrayOf(DashLength.toPx(), DashLength.toPx()))
                drawLine(DashedSeparatorColor, Offset(0f, 0f), Offset(size.width, 0f), 1.dp.toPx(), pathEffect = dashes)
            },
    )
}

/**
 * Leads to the archived tasks, which are not in the groups. Its description says what it opens, as the arrow alone
 * tells a screen reader nothing; the row grows with the font instead of clipping.
 */
@Composable
private fun ArchivedRow(
    count: Int,
    onClick: () -> Unit,
) {
    val description = pluralStringResource(R.plurals.tasks_list_archived_open, count, count)
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics(mergeDescendants = true) { contentDescription = description }
            .testTag("tasks_list_archived"),
        Arrangement.SpaceBetween,
        Alignment.CenterVertically,
    ) {
        Text(
            pluralStringResource(R.plurals.tasks_list_archived, count, count),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
