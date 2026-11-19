package io.github.ddiling.mydayboard.feature.counter.navigation

import io.github.ddiling.mydayboard.core.navigation.Destination
import kotlinx.serialization.Serializable

/** Route of the counter screen. It has no arguments, hence an object. */
@Serializable
data object CounterDestination : Destination
