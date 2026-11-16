package io.github.ddiling.mydayboard

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Surface
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import io.github.ddiling.mydayboard.core.designsystem.theme.AppTheme
import io.github.ddiling.mydayboard.feature.counter.CounterRoute

/**
 * The app shell stays thin: theme, navigation and the dashboard. Screens live in feature modules.
 * The dashboard will live here (only :app may depend on every feature), once there are cards to show.
 */
class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalComposeUiApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            AppTheme {
                Surface(
                    // Exposes Compose testTags as resource ids for Maestro/UiAutomator.
                    modifier = Modifier.fillMaxSize().semantics { testTagsAsResourceId = true },
                ) {
                    CounterRoute(modifier = Modifier.safeDrawingPadding())
                }
            }
        }
    }
}
