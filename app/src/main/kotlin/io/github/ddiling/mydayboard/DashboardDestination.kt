package io.github.ddiling.mydayboard

import io.github.ddiling.mydayboard.core.navigation.Destination
import kotlinx.serialization.Serializable

/** Route of the dashboard, the start destination. It has no arguments, hence an object. */
@Serializable
data object DashboardDestination : Destination
