package io.github.ddiling.mydayboard

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import io.github.ddiling.mydayboard.core.dashboard.DashboardCard
import io.github.ddiling.mydayboard.core.designsystem.theme.AppTheme
import java.time.Clock
import javax.inject.Inject

/**
 * The app shell stays thin: theme, navigation and the dashboard. Screens live in feature modules.
 * The dashboard lives here (only :app may depend on every feature); features contribute its cards via Hilt.
 *
 * `@AndroidEntryPoint` connects the activity to Hilt, so `hiltViewModel()` in the feature screens
 * it hosts can create their ViewModels with injected dependencies.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var dashboardCards: Set<@JvmSuppressWildcards DashboardCard>

    @Inject
    lateinit var clock: Clock

    @OptIn(ExperimentalComposeUiApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        // The app is dark only, so the bars are dark too, whatever the system setting is.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        setContent {
            AppTheme {
                Surface(
                    // Exposes Compose testTags as resource ids for Maestro/UiAutomator.
                    modifier = Modifier.fillMaxSize().semantics { testTagsAsResourceId = true },
                ) {
                    MyDayboardNavHost(navController = rememberNavController(), cards = dashboardCards, clock = clock)
                }
            }
        }
    }
}
