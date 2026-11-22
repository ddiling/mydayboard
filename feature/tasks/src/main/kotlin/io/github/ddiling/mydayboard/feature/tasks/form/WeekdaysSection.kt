package io.github.ddiling.mydayboard.feature.tasks.form

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.ddiling.mydayboard.core.domain.task.TaskFormError
import java.time.DayOfWeek
import java.time.format.TextStyle

private val DayButtonSize = 40.dp
private val MinTouchHeight = 48.dp

/**
 * Seven round buttons, Monday first. Each touch area takes an equal share of the row, so the areas never overlap and
 * are as wide as the screen allows (about 45 dp on a 360 dp phone, 48 dp or more on wider ones); the visible 40 dp
 * circle stays centred in it. The letter alone is ambiguous (two T, two S), so each button is announced
 * with the full weekday name, taken from the current locale.
 */
@Composable
internal fun WeekdaysSection(
    uiState: TaskFormUiState,
    actions: TaskFormActions,
) {
    val locale = LocalConfiguration.current.locales[0]
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth().testTag("tasks_form_weekdays")) {
            DayOfWeek.entries.forEach { day ->
                DayButton(
                    tag = "tasks_form_weekday_${day.value}",
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
private fun RowScope.DayButton(
    tag: String,
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
            .weight(1f)
            .heightIn(min = MinTouchHeight)
            .testTag(tag)
            .toggleable(value = on, role = Role.Checkbox, onValueChange = { onToggle() })
            .semantics { contentDescription = name },
        Alignment.Center,
    ) {
        Box(Modifier.size(DayButtonSize).clip(CircleShape).background(fill), Alignment.Center) {
            Text(letter, color = text, modifier = Modifier.clearAndSetSemantics {})
        }
    }
}

private const val OFF_ALPHA = 0.12f
