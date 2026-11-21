package io.github.ddiling.mydayboard.feature.trackers.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.ddiling.mydayboard.core.designsystem.theme.DashedSeparatorColor
import io.github.ddiling.mydayboard.feature.trackers.R
import io.github.ddiling.mydayboard.feature.trackers.list.asLastEventText

private val TileMinHeight = 84.dp
private val TileVerticalPadding = 12.dp
private val LeftTileEndPadding = 12.dp
private val RightTileStartPadding = 16.dp
private val DashLength = 4.dp
private val SeparatorWidth = 1.dp
private const val EMPTY_TILE_ALPHA = 0.6f

/**
 * Draws the grid's dashed lines with the tile itself: on the right edge of a left tile and the bottom edge of
 * a top-row tile. Because each tile draws its own share, a row that grows with a large font keeps full-length
 * lines, and there is no card background or border behind the grid.
 */
internal fun Modifier.dashedSeparators(
    isLeft: Boolean,
    isTopRow: Boolean,
): Modifier =
    drawBehind {
        val dashes = PathEffect.dashPathEffect(floatArrayOf(DashLength.toPx(), DashLength.toPx()))
        val width = SeparatorWidth.toPx()
        if (isLeft) {
            val x = size.width - width / 2
            drawLine(DashedSeparatorColor, Offset(x, 0f), Offset(x, size.height), width, pathEffect = dashes)
        }
        if (isTopRow) {
            val y = size.height - width / 2
            drawLine(DashedSeparatorColor, Offset(0f, y), Offset(size.width, y), width, pathEffect = dashes)
        }
    }

private fun Modifier.tilePadding(isLeft: Boolean): Modifier =
    heightIn(min = TileMinHeight).padding(
        start = if (isLeft) 0.dp else RightTileStartPadding,
        end = if (isLeft) LeftTileEndPadding else 0.dp,
        top = TileVerticalPadding,
        bottom = TileVerticalPadding,
    )

/**
 * A tap is the quick action (+1 or Reset) and a long press opens the detail. `combinedClickable` already buzzes on a long press, so the user feels
 * a different gesture; a second buzz here would double it. The click labels name both gestures for screen
 * readers, so TalkBack users can reach the detail too.
 */
@Composable
internal fun TrackerTile(
    row: TrackersDashboardRow,
    isLeft: Boolean,
    actions: TrackersDashboardCardActions,
    modifier: Modifier,
    onOpenDetail: () -> Unit,
) {
    val clickLabel =
        when (row) {
            is TrackersDashboardRow.Counter -> stringResource(R.string.trackers_card_plus_one_description, row.name)
            is TrackersDashboardRow.DaysSince -> stringResource(R.string.trackers_card_reset_description, row.name)
        }
    Column(
        modifier
            .combinedClickable(
                onClickLabel = clickLabel,
                onClick = {
                    when (row) {
                        is TrackersDashboardRow.Counter -> actions.onPlusOne(row.id)
                        is TrackersDashboardRow.DaysSince -> actions.onReset(row.id)
                    }
                },
                onLongClickLabel = stringResource(R.string.trackers_dashboard_open_details),
                onLongClick = {
                    onOpenDetail()
                },
            ).tilePadding(isLeft),
    ) {
        Text(row.name, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(
            row.detailText(),
            Modifier.padding(top = 4.dp),
            MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** A dim place that invites the user to add a tracker; 60% opacity keeps it quieter than real trackers. */
@Composable
internal fun EmptyTile(
    isLeft: Boolean,
    modifier: Modifier,
    onAdd: () -> Unit,
) {
    Row(
        modifier.clickable(onClick = onAdd).alpha(EMPTY_TILE_ALPHA).tilePadding(isLeft),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val muted = MaterialTheme.colorScheme.onSurfaceVariant
        Icon(PlusCircleIcon, contentDescription = null, Modifier.size(32.dp), muted)
        Text(stringResource(R.string.trackers_dashboard_add_slot), color = muted, fontSize = 12.sp)
    }
}

@Composable
private fun TrackersDashboardRow.detailText(): String =
    when (this) {
        is TrackersDashboardRow.Counter -> {
            // The count goes in as text, not %d, so the string needs no plural forms: "dziś" does not change.
            val count = todayCount.toString()
            if (sinceLast == null) {
                stringResource(R.string.trackers_dashboard_today, count)
            } else {
                stringResource(R.string.trackers_dashboard_today_ago, count, sinceLast.asLastEventText())
            }
        }

        is TrackersDashboardRow.DaysSince -> {
            pluralStringResource(R.plurals.trackers_days, days, days)
        }
    }
