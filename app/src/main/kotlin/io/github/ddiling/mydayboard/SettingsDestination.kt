package io.github.ddiling.mydayboard

import io.github.ddiling.mydayboard.core.navigation.Destination
import kotlinx.serialization.Serializable

/** Route of the settings placeholder. It has no arguments, hence an object. */
@Serializable
data object SettingsDestination : Destination
