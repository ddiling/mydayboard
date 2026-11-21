package io.github.ddiling.mydayboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.ddiling.mydayboard.core.dashboard.DashboardCard
import io.github.ddiling.mydayboard.core.dashboard.DashboardHost

/**
 * The dashboard: the header, then the cards (already in display order), or a hint when there are none.
 * The header's settings icon opens the settings placeholder.
 */
@Composable
fun DashboardScreen(
    headerTime: HeaderTime,
    cards: List<DashboardCard>,
    host: DashboardHost,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(host.snackbarHostState) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item(key = "header") {
                DashboardHeader(headerTime, onOpenSettings = { host.navigate(SettingsDestination) })
            }
            if (cards.isEmpty()) {
                item(key = "empty") { Text(stringResource(R.string.dashboard_empty)) }
            }
            items(cards, key = { it.key }) { card ->
                card.Content(host = host, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}
