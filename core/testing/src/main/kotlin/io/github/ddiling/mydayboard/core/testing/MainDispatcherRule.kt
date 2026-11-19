package io.github.ddiling.mydayboard.core.testing

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/**
 * Replaces [Dispatchers.Main] with [dispatcher] for each test and restores it afterwards:
 * `@get:Rule val mainDispatcherRule = MainDispatcherRule()`. ViewModels launch on `viewModelScope`, which
 * uses [Dispatchers.Main], and a JVM unit test has no Android main thread. The default [UnconfinedTestDispatcher]
 * runs launched code straight away; pass a `StandardTestDispatcher` to decide when queued work runs.
 * The opt-in is for `setMain`/`resetMain`, still marked experimental although they are the documented way
 * to do this; having it here spares every test from repeating it.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
    val dispatcher: TestDispatcher = UnconfinedTestDispatcher(),
) : TestWatcher() {
    override fun starting(description: Description) {
        Dispatchers.setMain(dispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}
