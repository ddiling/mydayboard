package io.github.ddiling.mydayboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.ddiling.mydayboard.core.dashboard.DashboardCard
import io.github.ddiling.mydayboard.core.dashboard.DashboardHost
import io.github.ddiling.mydayboard.feature.counter.navigation.CounterDestination
import io.github.ddiling.mydayboard.feature.trackers.navigation.TrackersListDestination

internal const val OPEN_TRACKERS_TAG = "open_trackers"
internal const val OPEN_COUNTER_TAG = "open_counter"

/**
 * The dashboard: the cards (already in display order) in a list, or a hint when there are none.
 * The first top-bar action opens the tracker list; the second is a temporary route to the counter placeholder
 * and goes away with it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    cards: List<DashboardCard>,
    host: DashboardHost,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    TextButton(
                        onClick = { host.navigate(TrackersListDestination) },
                        modifier = Modifier.testTag(OPEN_TRACKERS_TAG),
                    ) { Text(stringResource(R.string.dashboard_open_trackers)) }
                    TextButton(
                        onClick = { host.navigate(CounterDestination) },
                        modifier = Modifier.testTag(OPEN_COUNTER_TAG),
                    ) { Text(stringResource(R.string.dashboard_open_counter)) }
                },
            )
        },
        snackbarHost = { SnackbarHost(host.snackbarHostState) },
    ) { padding ->
        if (cards.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.dashboard_empty))
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                items(cards, key = { it.key }) { card ->
                    card.Content(host = host, modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}
