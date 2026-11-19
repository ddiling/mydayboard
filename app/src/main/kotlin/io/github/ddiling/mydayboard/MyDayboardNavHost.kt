package io.github.ddiling.mydayboard

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import io.github.ddiling.mydayboard.feature.counter.navigation.CounterDestination
import io.github.ddiling.mydayboard.feature.counter.navigation.counterScreen

/**
 * The navigation graph of the app. Each feature contributes its destinations as a
 * `NavGraphBuilder` extension; this is the only place that lists them.
 * The controller is a parameter so tests can pass a `TestNavHostController`.
 */
@Composable
fun MyDayboardNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = CounterDestination,
        modifier = modifier,
    ) {
        counterScreen()
    }
}
