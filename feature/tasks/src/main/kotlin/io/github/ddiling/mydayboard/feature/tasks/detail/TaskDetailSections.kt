package io.github.ddiling.mydayboard.feature.tasks.detail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.unit.dp
import io.github.ddiling.mydayboard.core.designsystem.theme.TaskColors
import io.github.ddiling.mydayboard.feature.tasks.R
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

private val timeFormat = DateTimeFormatter.ofPattern("HH:mm")
private val ButtonHeight = 52.dp

/** A ticked time is filled with a tick; tapping it unticks, tapping an open one ticks it. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun TimeChips(
    chips: List<TimeChip>,
    onChipClick: (LocalTime) -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier.fillMaxWidth().padding(bottom = 16.dp),
        Arrangement.spacedBy(8.dp),
        Arrangement.spacedBy(8.dp),
    ) {
        chips.forEach { chip -> TimeChipView(chip) { onChipClick(chip.time) } }
    }
}

@Composable
private fun TimeChipView(
    chip: TimeChip,
    onClick: () -> Unit,
) {
    val time = chip.time.format(timeFormat)
    val description =
        stringResource(if (chip.ticked) R.string.tasks_detail_chip_ticked else R.string.tasks_detail_chip_open, time)
    val colors = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        color = if (chip.ticked) colors.primaryContainer else colors.surfaceVariant,
        modifier =
            Modifier
                .heightIn(min = 48.dp)
                .semantics { contentDescription = description }
                .testTag("tasks_detail_chip_$time"),
    ) {
        Row(Modifier.padding(horizontal = 16.dp), Arrangement.spacedBy(6.dp), Alignment.CenterVertically) {
            if (chip.ticked) Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
            Text(time, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(vertical = 12.dp))
        }
    }
}

/** The section title; a heading so screen readers can jump to it. */
@Composable
internal fun HistoryTitle() {
    Text(
        stringResource(R.string.tasks_detail_history_title).uppercase(Locale.current.platformLocale),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier =
            Modifier
                .heightIn(min = 48.dp)
                .wrapContentHeight(Alignment.CenterVertically)
                .semantics { heading() }
                .testTag("tasks_detail_history_title"),
    )
}

/** One past day: the day on the left, the result on the right. Read as "Yesterday, Missed. Tap to change". */
@Composable
internal fun HistoryRowLine(
    row: HistoryRow,
    today: LocalDate,
    onClick: () -> Unit,
) {
    val resources = LocalResources.current
    val day = historyDayText(resources, row.day, today)
    val result = historyResultText(resources, row.result)
    val description = stringResource(R.string.tasks_detail_history_row, day, result)
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics(mergeDescendants = true) { contentDescription = description }
            .testTag("tasks_detail_history_${row.day}"),
        Arrangement.SpaceBetween,
        Alignment.CenterVertically,
    ) {
        Text(day, style = MaterialTheme.typography.bodyMedium)
        Text(result, style = MaterialTheme.typography.bodyMedium, color = resultColor(row.result))
    }
}

@Composable
private fun resultColor(result: HistoryResult): Color =
    when {
        result == HistoryResult.Done -> TaskColors.done
        result == HistoryResult.Missed -> TaskColors.late
        result is HistoryResult.Partial && result.settled -> TaskColors.late
        else -> MaterialTheme.colorScheme.onSurface
    }

/**
 * The action at the bottom. A task that is not due has no button, only "Next: Sat 28.11". Untick is a quiet
 * outlined button, so taking a tick back is never the loudest thing on the screen.
 */
@Composable
internal fun BottomAction(
    content: TaskDetailUiState.Content,
    onBigButton: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val resources = LocalResources.current
    val text = bigButtonText(resources, content.bigButton)
    val nextDue = content.nextDue
    Column(modifier.fillMaxWidth().padding(20.dp)) {
        when {
            text != null -> {
                val buttonModifier =
                    Modifier.fillMaxWidth().heightIn(min = ButtonHeight).testTag("tasks_detail_big_button")
                if (content.bigButton == BigButton.UntickToday) {
                    OutlinedButton(onClick = onBigButton, modifier = buttonModifier) { Text(text) }
                } else {
                    Button(
                        onClick = onBigButton,
                        modifier = buttonModifier,
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            ),
                    ) { Text(text) }
                }
            }

            nextDue != null -> {
                Text(
                    nextDueText(resources, nextDue),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.heightIn(min = ButtonHeight).testTag("tasks_detail_next"),
                )
            }
        }
    }
}
