package io.github.ddiling.mydayboard

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Starts [MainActivity] the way a device does: with the application from the manifest, not a test one.
 *
 * [MainActivityTest] swaps in `HiltTestApplication`, so it cannot notice a missing `@HiltAndroidApp` on
 * [MyDayboardApplication] or a missing `android:name` in the manifest. Either mistake compiles, but an
 * `@AndroidEntryPoint` activity then crashes on start, and this test fails.
 */
@RunWith(RobolectricTestRunner::class)
class AppStartTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun `the app starts through its Hilt application and shows the dashboard`() {
        assertTrue(composeRule.activity.application is MyDayboardApplication)

        composeRule.onNodeWithTag(OPEN_TRACKERS_TAG).assertIsDisplayed()
    }
}
