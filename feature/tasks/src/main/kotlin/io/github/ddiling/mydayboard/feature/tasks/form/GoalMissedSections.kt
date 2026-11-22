package io.github.ddiling.mydayboard.feature.tasks.form

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import io.github.ddiling.mydayboard.core.domain.task.OnMiss
import io.github.ddiling.mydayboard.core.domain.task.TaskFormError
import io.github.ddiling.mydayboard.feature.tasks.R

@Composable
private fun SectionHeading(text: String) {
    Text(text, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun HelpText(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

/**
 * The optional count goal. A several-times-a-day task has none, because a tick per time already is its count, so
 * the section is left out there instead of offering a switch that would do nothing.
 */
@Composable
internal fun GoalSection(
    uiState: TaskFormUiState,
    actions: TaskFormActions,
) {
    if (uiState.repeatType.hidesGoal()) return
    val goalError = uiState.errors.firstOrNull { it is TaskFormError.GoalOutOfRange }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionHeading(stringResource(R.string.tasks_form_goal_title))
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .toggleable(uiState.goalOn, role = Role.Switch, onValueChange = actions.onGoalOnChange)
                .testTag("tasks_form_goal_switch"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(R.string.tasks_form_goal_switch), Modifier.weight(1f))
            Switch(checked = uiState.goalOn, onCheckedChange = null)
        }
        HelpText(stringResource(R.string.tasks_form_goal_help))
        if (uiState.goalOn) {
            DashedNumberField(
                label = stringResource(R.string.tasks_form_goal),
                value = uiState.goal,
                onValueChange = actions.onGoalChange,
                error = goalError.message(),
                modifier = Modifier.testTag("tasks_form_goal"),
            )
        }
    }
}

/**
 * What happens to a task that was not done in time. Daily types start a new occurrence at midnight, so the choice
 * would change nothing and the section is hidden there.
 */
@Composable
internal fun MissedSection(
    uiState: TaskFormUiState,
    actions: TaskFormActions,
) {
    if (uiState.repeatType.hidesStays()) return
    Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionHeading(stringResource(R.string.tasks_form_missed_title))
        RadioOption(
            selected = uiState.onMiss == OnMiss.STAYS,
            onSelect = { actions.onOnMissChange(OnMiss.STAYS) },
            label = stringResource(R.string.tasks_form_missed_stays),
            help = stringResource(R.string.tasks_form_missed_stays_help),
            tag = "tasks_form_missed_stays",
        )
        RadioOption(
            selected = uiState.onMiss == OnMiss.EXPIRES,
            onSelect = { actions.onOnMissChange(OnMiss.EXPIRES) },
            label = stringResource(R.string.tasks_form_missed_expires),
            help = stringResource(R.string.tasks_form_missed_expires_help),
            tag = "tasks_form_missed_expires",
        )
        HelpText(stringResource(R.string.tasks_form_missed_note))
    }
}
