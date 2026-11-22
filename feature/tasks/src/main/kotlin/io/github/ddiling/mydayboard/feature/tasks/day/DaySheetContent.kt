package io.github.ddiling.mydayboard.feature.tasks.day

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.ddiling.mydayboard.core.designsystem.component.PrimaryButton
import io.github.ddiling.mydayboard.feature.tasks.R
import io.github.ddiling.mydayboard.feature.tasks.detail.TimeChip
import io.github.ddiling.mydayboard.feature.tasks.detail.TimeChips
import java.time.LocalTime

/** The sheet around [DaySheetContent]; swipe down, a tap outside and Back all call [DaySheetActions.onDismiss]. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DaySheetModal(
    state: DaySheetState,
    actions: DaySheetActions,
) {
    ModalBottomSheet(onDismissRequest = actions.onDismiss) { DaySheetContent(state, actions) }
}

@Composable
internal fun DaySheetContent(
    state: DaySheetState,
    actions: DaySheetActions,
    modifier: Modifier = Modifier,
) {
    val resources = LocalResources.current
    Column(modifier.fillMaxWidth().padding(horizontal = 20.dp), Arrangement.spacedBy(8.dp)) {
        Text(
            daySheetTitle(resources, state),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.semantics { heading() }.testTag("tasks_day_title"),
        )
        Hint(R.string.tasks_day_hint)
        when (val kind = state.kind) {
            is DaySheetKind.Single -> SingleChoice(kind.done, actions.onSetSingle)
            is DaySheetKind.Times -> TimesChoice(kind, actions.onToggle)
            is DaySheetKind.Count -> CountStepper(kind, actions.onStep)
        }
        PrimaryButton(
            stringResource(R.string.tasks_day_save),
            actions.onSave,
            Modifier.fillMaxWidth().height(52.dp).testTag("tasks_day_save"),
        )
    }
}

@Composable
private fun Hint(text: Int) {
    Text(
        stringResource(text),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun SingleChoice(
    done: Boolean,
    onSetSingle: (Boolean) -> Unit,
) {
    Column(Modifier.selectableGroup()) {
        RadioRow(R.string.tasks_day_not_done, selected = !done, tag = "tasks_day_not_done") { onSetSingle(false) }
        RadioRow(R.string.tasks_day_done, selected = done, tag = "tasks_day_done") { onSetSingle(true) }
    }
}

/** The row is the radio's accessible name and its whole width is the target; the button itself only draws. */
@Composable
private fun RadioRow(
    label: Int,
    selected: Boolean,
    tag: String,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .selectable(selected, role = Role.RadioButton, onClick = onClick)
            .testTag(tag),
        Arrangement.spacedBy(12.dp),
        Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null)
        Text(stringResource(label), style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun TimesChoice(
    kind: DaySheetKind.Times,
    onToggle: (LocalTime) -> Unit,
) {
    Hint(R.string.tasks_day_times_hint)
    TimeChips(
        kind.slots.map { TimeChip(it, ticked = it in kind.chosen) },
        onToggle,
        Modifier.testTag("tasks_day_chips"),
    )
}

@Composable
private fun CountStepper(
    kind: DaySheetKind.Count,
    onStep: (Int) -> Unit,
) {
    Hint(R.string.tasks_day_count_hint)
    Row(Modifier.testTag("tasks_day_stepper"), Arrangement.spacedBy(16.dp), Alignment.CenterVertically) {
        StepButton("\u2212", R.string.tasks_day_less, kind.value > 0, "tasks_day_minus") { onStep(-1) }
        Text(
            stringResource(R.string.tasks_day_count, kind.value, kind.target),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.testTag("tasks_day_count"),
        )
        StepButton("+", R.string.tasks_day_more, kind.value < kind.target, "tasks_day_plus") { onStep(+1) }
    }
}

/** A 48 dp button drawn with a sign, because the core icon set has no minus; the description is its name. */
@Composable
private fun StepButton(
    sign: String,
    description: Int,
    enabled: Boolean,
    tag: String,
    onClick: () -> Unit,
) {
    val name = stringResource(description)
    IconButton(onClick, Modifier.size(48.dp).semantics { contentDescription = name }.testTag(tag), enabled) {
        Text(sign, style = MaterialTheme.typography.titleLarge)
    }
}
