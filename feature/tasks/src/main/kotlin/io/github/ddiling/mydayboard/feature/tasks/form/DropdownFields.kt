package io.github.ddiling.mydayboard.feature.tasks.form

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import io.github.ddiling.mydayboard.core.domain.task.TaskCategory
import io.github.ddiling.mydayboard.feature.tasks.R

/** One entry of a dropdown: [value] is what the callback gets, [text] what the user reads. */
private data class DropdownOption<T>(
    val value: T,
    val text: String,
)

@Composable
private fun <T> DashedDropdown(
    label: String,
    selected: String,
    options: List<DropdownOption<T>>,
    onSelect: (T) -> Unit,
    tag: String,
) {
    var expanded by remember { mutableStateOf(false) }
    DashedField(label) {
        Box {
            Row(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .clickable(role = Role.DropdownList) { expanded = true }
                    .testTag(tag),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(selected, Modifier.weight(1f))
                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option.text) },
                        onClick = {
                            expanded = false
                            onSelect(option.value)
                        },
                    )
                }
            }
        }
    }
}

/** "No category" is an entry of its own, so a task can be moved out of a category again. */
@Composable
internal fun CategoryField(
    categoryId: String?,
    categories: List<TaskCategory>,
    onCategoryChange: (String?) -> Unit,
) {
    val none = stringResource(R.string.tasks_form_category_none)
    val options =
        listOf(DropdownOption<String?>(null, none)) + categories.map { DropdownOption<String?>(it.id, it.name) }
    DashedDropdown(
        label = stringResource(R.string.tasks_form_category),
        selected = options.firstOrNull { it.value == categoryId }?.text ?: none,
        options = options,
        onSelect = onCategoryChange,
        tag = "tasks_form_category",
    )
}

@Composable
internal fun RepeatField(
    repeatType: RepeatType,
    onRepeatTypeChange: (RepeatType) -> Unit,
) {
    DashedDropdown(
        label = stringResource(R.string.tasks_form_repeat),
        selected = stringResource(repeatType.label()),
        options = availableRepeatTypes.map { DropdownOption(it, stringResource(it.label())) },
        onSelect = onRepeatTypeChange,
        tag = "tasks_form_repeat",
    )
}

private fun RepeatType.label(): Int =
    when (this) {
        RepeatType.TIMES_PER_DAY -> R.string.tasks_form_repeat_times_per_day
        RepeatType.EVERY_DAY -> R.string.tasks_form_repeat_every_day
        RepeatType.WEEKDAYS -> R.string.tasks_form_repeat_weekdays
        RepeatType.EVERY_N_DAYS -> R.string.tasks_form_repeat_every_n_days
        RepeatType.MONTHLY -> R.string.tasks_form_repeat_monthly
        RepeatType.ONE_TIME -> R.string.tasks_form_repeat_one_time
    }
