package io.github.ddiling.mydayboard.feature.tasks.form

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.PreviewLightDark
import io.github.ddiling.mydayboard.core.domain.task.MAX_TASK_GOAL
import io.github.ddiling.mydayboard.core.domain.task.MIN_TASK_GOAL
import io.github.ddiling.mydayboard.core.domain.task.OnMiss
import io.github.ddiling.mydayboard.core.domain.task.TaskFormError

@PreviewLightDark
@Composable
private fun TaskFormGoalAndMissedPreview() =
    PreviewForm(
        emptyForm.copy(
            name = "Send CVs",
            repeatType = RepeatType.ONE_TIME,
            dueDate = previewToday.plusDays(7),
            goalOn = true,
            goal = 5,
            onMiss = OnMiss.STAYS,
            canSave = true,
        ),
    )

@PreviewLightDark
@Composable
private fun TaskFormGoalErrorPreview() =
    PreviewForm(
        emptyForm.copy(
            name = "Water",
            goalOn = true,
            goal = 1,
            errors = listOf(TaskFormError.GoalOutOfRange(MIN_TASK_GOAL, MAX_TASK_GOAL)),
        ),
    )
