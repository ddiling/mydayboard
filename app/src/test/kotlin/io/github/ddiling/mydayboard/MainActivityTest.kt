package io.github.ddiling.mydayboard

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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
 * Hilt checks the graph at compile time, but only this test proves it is wired at runtime: the
 * activity is an entry point, and `hiltViewModel()` creates CounterViewModel with its injected use case.
 * A missing `@AndroidEntryPoint` or binding makes the activity crash on start instead of showing the button.
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

    @Test
    fun `the counter screen is shown and counts a tap`() {
        composeRule.onNodeWithTag("tap_button").assertIsDisplayed()

        composeRule.onNodeWithTag("tap_button").performClick()

        composeRule.onNodeWithText("You tapped once").assertIsDisplayed()
    }
}
