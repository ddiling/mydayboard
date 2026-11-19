package io.github.ddiling.mydayboard.core.testing

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.Description
import org.junit.runners.model.Statement

// The rule is applied by hand (not with @Rule) so a test can also look at what happens after it finished.
class MainDispatcherRuleTest {
    private fun MainDispatcherRule.runAround(body: () -> Unit) =
        apply(
            object : Statement() {
                override fun evaluate() = body()
            },
            Description.EMPTY,
        ).evaluate()

    @Test
    fun `code launched on Main runs on the rule's dispatcher during the test`() {
        val rule = MainDispatcherRule(StandardTestDispatcher())
        var ran = false

        rule.runAround {
            CoroutineScope(Dispatchers.Main).launch { ran = true }
            rule.dispatcher.scheduler.advanceUntilIdle() // a StandardTestDispatcher runs nothing until told to
        }

        assertTrue(ran)
    }

    @Test
    fun `Main is reset after the test`() =
        runTest {
            MainDispatcherRule().runAround {}

            val failure = runCatching { withContext(Dispatchers.Main) {} }.exceptionOrNull()

            assertTrue("expected the missing-Main error, got $failure", failure is IllegalStateException)
        }
}
