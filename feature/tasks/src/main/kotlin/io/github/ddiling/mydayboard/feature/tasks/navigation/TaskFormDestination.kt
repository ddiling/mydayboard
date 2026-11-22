package io.github.ddiling.mydayboard.feature.tasks.navigation

import io.github.ddiling.mydayboard.core.navigation.Destination
import kotlinx.serialization.Serializable

/** Route of the task form: [taskId] null creates a task, otherwise edits that one. */
@Serializable
data class TaskFormDestination(
    val taskId: String? = null,
) : Destination
