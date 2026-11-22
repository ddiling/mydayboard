package io.github.ddiling.mydayboard.feature.tasks.navigation

import io.github.ddiling.mydayboard.core.navigation.Destination
import kotlinx.serialization.Serializable

/** Route of the full tasks list, grouped by category. */
@Serializable
data object TasksListDestination : Destination
