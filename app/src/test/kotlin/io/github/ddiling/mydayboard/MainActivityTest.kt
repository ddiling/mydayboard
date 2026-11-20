package io.github.ddiling.mydayboard

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Starts the real [MainActivity] with the real Hilt graph on the JVM.
 *
 * Hilt checks the graph at compile time (a missing binding fails `:app` compilation), but only this test
 * proves it is wired at runtime: the activity is an entry point, and `hiltViewModel()` creates
 * the dashboard with the trackers card contributed through Hilt multibinding. A missing `@AndroidEntryPoint`
 * (on the activity) or a missing card binding still compiles, but crashes on start or shows no card.
 * The application itself is replaced by `HiltTestApplication` here; [AppStartTest] covers it.
 */
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@Config(application = HiltTestApplication::class)
class MainActivityTest {
    // Hilt must create the test component before the activity starts, hence the order.
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    private val emptyHint = "No trackers yet. Add one to start tracking."

    @Test
    fun `the dashboard shows the trackers card contributed through Hilt`() {
        // The card loads its trackers from Room on a background thread, so wait for the empty state.
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText(emptyHint).fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithText(emptyHint).assertIsDisplayed()
    }
}
