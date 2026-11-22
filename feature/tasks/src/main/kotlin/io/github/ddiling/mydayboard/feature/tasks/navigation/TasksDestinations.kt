package io.github.ddiling.mydayboard.feature.tasks.navigation

import io.github.ddiling.mydayboard.core.navigation.Destination
import kotlinx.serialization.Serializable

@Serializable
data object TasksListDestination : Destination

@Serializable
data class TaskDetailDestination(
    val taskId: String,
) : Destination
