package io.github.ddiling.mydayboard.feature.tasks.navigation

import io.github.ddiling.mydayboard.core.navigation.Destination
import kotlinx.serialization.Serializable

/** Route of the task details screen for the task with [taskId]. */
@Serializable
data class TaskDetailDestination(
    val taskId: String,
) : Destination
