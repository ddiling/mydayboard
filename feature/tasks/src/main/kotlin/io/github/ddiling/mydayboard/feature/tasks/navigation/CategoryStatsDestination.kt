package io.github.ddiling.mydayboard.feature.tasks.navigation

import io.github.ddiling.mydayboard.core.navigation.Destination
import kotlinx.serialization.Serializable

/** Route of the statistics screen of the category with [categoryId]. */
@Serializable
data class CategoryStatsDestination(
    val categoryId: String,
) : Destination
