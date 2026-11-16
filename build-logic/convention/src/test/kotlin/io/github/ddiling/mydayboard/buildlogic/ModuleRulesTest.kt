package io.github.ddiling.mydayboard.buildlogic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ModuleRulesTest {
    @Test
    fun `the app may depend on features and core`() {
        assertTrue(ModuleRules.violations(":app", listOf(":feature:counter", ":core:domain")).isEmpty())
    }

    @Test
    fun `features may depend on core`() {
        assertTrue(ModuleRules.violations(":feature:counter", listOf(":core:designsystem")).isEmpty())
    }

    @Test
    fun `core may depend on other core modules`() {
        assertTrue(ModuleRules.violations(":core:designsystem", listOf(":core:domain")).isEmpty())
    }

    @Test
    fun `a feature must not depend on another feature`() {
        assertEquals(1, ModuleRules.violations(":feature:counter", listOf(":feature:alarms")).size)
    }

    @Test
    fun `a feature must not depend on the app`() {
        assertEquals(1, ModuleRules.violations(":feature:counter", listOf(":app")).size)
    }

    @Test
    fun `core must not depend on features or the app`() {
        assertEquals(2, ModuleRules.violations(":core:domain", listOf(":feature:counter", ":app")).size)
    }

    @Test
    fun `a module named like the app but nested is not the app`() {
        // ":application" and ":app:legacy" are not ":app", so they are rejected as unknown modules.
        assertEquals(1, ModuleRules.violations(":application", emptyList()).size)
        assertEquals(1, ModuleRules.violations(":app:legacy", emptyList()).size)
    }

    @Test
    fun `modules outside the layers are rejected`() {
        assertEquals(1, ModuleRules.violations(":legacy", emptyList()).size)
    }
}
