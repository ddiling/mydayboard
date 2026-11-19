package io.github.ddiling.mydayboard.feature.counter.navigation

import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import io.github.ddiling.mydayboard.feature.counter.CounterRoute

/**
 * Adds the counter screen to a graph. The app calls this instead of knowing the screen's
 * internals, so a feature can change its screens without touching `:app`.
 */
fun NavGraphBuilder.counterScreen() {
    composable<CounterDestination> {
        CounterRoute(modifier = Modifier.safeDrawingPadding())
    }
}
