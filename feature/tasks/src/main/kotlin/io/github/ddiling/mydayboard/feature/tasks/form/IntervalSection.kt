package io.github.ddiling.mydayboard.feature.tasks.form

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.ddiling.mydayboard.core.domain.task.EveryNDaysMode
import io.github.ddiling.mydayboard.core.domain.task.MAX_INTERVAL_DAYS
import io.github.ddiling.mydayboard.core.domain.task.MIN_INTERVAL_DAYS
import io.github.ddiling.mydayboard.feature.tasks.R
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * "Every N days": the stepper, the two ways to count, and the start date that only the fixed calendar needs (the
 * other way counts from the last tick, so a start date would change nothing). The stepper buttons switch off at
 * the ends of the range instead of silently ignoring a tap.
 */
@Composable
internal fun IntervalSection(
    uiState: TaskFormUiState,
    actions: TaskFormActions,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Stepper(uiState.intervalDays, actions.onIntervalChange)
        RadioOption(
            selected = uiState.intervalMode == EveryNDaysMode.FROM_LAST_DONE,
            onSelect = { actions.onIntervalModeChange(EveryNDaysMode.FROM_LAST_DONE) },
            label = stringResource(R.string.tasks_form_mode_from_last),
            help = stringResource(R.string.tasks_form_mode_from_last_help),
            tag = "tasks_form_mode_from_last",
        )
        RadioOption(
            selected = uiState.intervalMode == EveryNDaysMode.FIXED_CALENDAR,
            onSelect = { actions.onIntervalModeChange(EveryNDaysMode.FIXED_CALENDAR) },
            label = stringResource(R.string.tasks_form_mode_fixed),
            help = stringResource(R.string.tasks_form_mode_fixed_help),
            tag = "tasks_form_mode_fixed",
        )
        if (uiState.intervalMode == EveryNDaysMode.FIXED_CALENDAR) {
            DateField(
                label = stringResource(R.string.tasks_form_starting_on),
                date = uiState.firstDue,
                opensOn = uiState.today,
                onDateChange = actions.onFirstDueChange,
                tag = "tasks_form_first_due",
            )
        }
        NextDates(uiState.nextDueDates())
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Stepper(
    days: Int,
    onChange: (Int) -> Unit,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.Center,
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        Text(stringResource(R.string.tasks_form_interval_every), style = MaterialTheme.typography.bodyLarge)
        StepButton(
            "−",
            stringResource(R.string.tasks_form_interval_less),
            days > MIN_INTERVAL_DAYS,
            "tasks_form_interval_less",
        ) {
            onChange(days - 1)
        }
        Text(
            days.toString(),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.testTag("tasks_form_interval"),
        )
        StepButton(
            "+",
            stringResource(R.string.tasks_form_interval_more),
            days < MAX_INTERVAL_DAYS,
            "tasks_form_interval_more",
        ) {
            onChange(days + 1)
        }
        Text(pluralStringResource(R.plurals.tasks_form_interval_unit, days), style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun StepButton(
    symbol: String,
    description: String,
    enabled: Boolean,
    tag: String,
    onClick: () -> Unit,
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.testTag(tag).semantics { contentDescription = description },
    ) { Text(symbol, Modifier.clearAndSetSemantics {}, style = MaterialTheme.typography.titleLarge) }
}

/** The info line under the options: the dates, in the same style as the date fields. Nothing when there are none. */
@Composable
private fun NextDates(dates: List<LocalDate>) {
    if (dates.isEmpty()) return
    val locale = LocalConfiguration.current.locales[0]
    val format = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale)
    Text(
        stringResource(R.string.tasks_form_next_dates, dates.joinToString { it.format(format) }),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.testTag("tasks_form_next_dates"),
    )
}
