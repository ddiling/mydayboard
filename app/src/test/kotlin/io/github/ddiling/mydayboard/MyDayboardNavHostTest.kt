package io.github.ddiling.mydayboard

import android.content.ComponentName
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.testing.TestNavHostController
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import io.github.ddiling.mydayboard.feature.counter.navigation.CounterDestination
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * The start destination creates its ViewModel with `hiltViewModel()`, which only works inside an
 * `@AndroidEntryPoint` activity. This test needs its own controller (a `TestNavHostController`), so it
 * cannot reuse [MainActivity], which builds its own. [HiltTestActivity] is that host. It lives in the
 * test sources, so it is registered with Robolectric here instead of in a manifest that would ship in the app.
 */
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@Config(application = HiltTestApplication::class)
class MyDayboardNavHostTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createEmptyComposeRule()

    @Test
    fun `the app starts on the counter destination`() {
        val context = ApplicationProvider.getApplicationContext<HiltTestApplication>()
        shadowOf(context.packageManager).addActivityIfNotPresent(ComponentName(context, HiltTestActivity::class.java))
        lateinit var navController: TestNavHostController

        ActivityScenario.launch(HiltTestActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.setContent {
                    navController = TestNavHostController(LocalContext.current)
                    navController.navigatorProvider.addNavigator(ComposeNavigator())
                    MyDayboardNavHost(navController = navController)
                }
            }
            composeRule.waitForIdle()

            assertTrue(navController.currentBackStackEntry!!.destination.hasRoute<CounterDestination>())
        }
    }

    @AndroidEntryPoint
    class HiltTestActivity : ComponentActivity()
}
