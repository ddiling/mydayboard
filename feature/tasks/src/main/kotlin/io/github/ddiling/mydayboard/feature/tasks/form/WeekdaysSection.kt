package io.github.ddiling.mydayboard.feature.tasks.form

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.ddiling.mydayboard.core.domain.task.TaskFormError
import java.time.DayOfWeek
import java.time.format.TextStyle

private val DayButtonSize = 40.dp

/**
 * Seven round buttons, Monday first. The letter alone is ambiguous (two T, two S), so each button is announced
 * with the full weekday name, taken from the current locale.
 */
@Composable
internal fun WeekdaysSection(
    uiState: TaskFormUiState,
    actions: TaskFormActions,
) {
    val locale = LocalConfiguration.current.locales[0]
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
            DayOfWeek.entries.forEach { day ->
                DayButton(
                    letter = day.getDisplayName(TextStyle.NARROW, locale),
                    name = day.getDisplayName(TextStyle.FULL, locale),
                    on = day in uiState.weekdays,
                    onToggle = { actions.onToggleWeekday(day) },
                )
            }
        }
        SectionError(TaskFormError.NoWeekdays, TaskFormError.NoWeekdays in uiState.errors)
    }
}

@Composable
private fun DayButton(
    letter: String,
    name: String,
    on: Boolean,
    onToggle: () -> Unit,
) {
    val fill =
        if (on) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurface.copy(
                alpha = OFF_ALPHA,
            )
        }
    val text = if (on) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    Box(
        Modifier
            .size(DayButtonSize)
            .clip(CircleShape)
            .background(fill)
            .toggleable(value = on, role = Role.Checkbox, onValueChange = { onToggle() })
            .semantics { contentDescription = name },
        Alignment.Center,
    ) { Text(letter, color = text, modifier = Modifier.clearAndSetSemantics {}) }
}

private const val OFF_ALPHA = 0.12f
