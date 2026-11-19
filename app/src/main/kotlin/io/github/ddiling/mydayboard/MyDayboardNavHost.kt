package io.github.ddiling.mydayboard

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import io.github.ddiling.mydayboard.core.dashboard.DashboardCard
import io.github.ddiling.mydayboard.core.dashboard.sortedDashboardCards
import io.github.ddiling.mydayboard.feature.counter.navigation.counterScreen

/**
 * The navigation graph of the app. Each feature contributes its destinations as a
 * `NavGraphBuilder` extension; this is the only place that lists them. The dashboard is the start.
 * The controller is a parameter so tests can pass a `TestNavHostController`.
 */
@Composable
fun MyDayboardNavHost(
    navController: NavHostController,
    cards: Set<DashboardCard>,
    modifier: Modifier = Modifier,
) {
    val host = remember(navController) { NavDashboardHost(navController) }
    val sortedCards = remember(cards) { sortedDashboardCards(cards) }
    NavHost(
        navController = navController,
        startDestination = DashboardDestination,
        modifier = modifier,
    ) {
        composable<DashboardDestination> {
            DashboardScreen(cards = sortedCards, host = host)
        }
        counterScreen()
    }
}
