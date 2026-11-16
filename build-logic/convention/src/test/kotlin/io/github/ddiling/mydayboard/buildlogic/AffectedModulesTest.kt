package io.github.ddiling.mydayboard.buildlogic

import org.junit.Assert.assertEquals
import org.junit.Test

class AffectedModulesTest {
    // Mirrors the real graph: app -> features -> core.
    private val modules =
        listOf(
            ModuleInfo(":app", "app", setOf(":feature:counter", ":feature:alarms", ":core:designsystem")),
            ModuleInfo(":feature:counter", "feature/counter", setOf(":core:designsystem", ":core:domain")),
            ModuleInfo(":feature:alarms", "feature/alarms", setOf(":core:designsystem")),
            ModuleInfo(":core:designsystem", "core/designsystem", emptySet()),
            ModuleInfo(":core:domain", "core/domain", emptySet()),
        )

    private val everything = modules.map { it.path }.toSortedSet()

    private fun affected(vararg files: String) = AffectedModules.calculate(modules, files.toList()).modules

    @Test
    fun `no changed file list means everything`() {
        assertEquals(everything, AffectedModules.calculate(modules, null).modules)
    }

    @Test
    fun `an app change affects only the app`() {
        assertEquals(setOf(":app"), affected("app/src/main/kotlin/A.kt"))
    }

    @Test
    fun `a feature change affects the feature and the app`() {
        assertEquals(setOf(":feature:alarms", ":app"), affected("feature/alarms/src/main/kotlin/A.kt"))
        assertEquals(setOf(":feature:counter", ":app"), affected("feature/counter/build.gradle.kts"))
    }

    @Test
    fun `a core change affects every module that depends on it transitively`() {
        assertEquals(
            setOf(":core:domain", ":feature:counter", ":app"),
            affected("core/domain/src/main/kotlin/A.kt"),
        )
        // :core:domain does not depend on the design system, so it is not affected.
        assertEquals(everything - ":core:domain", affected("core/designsystem/src/main/kotlin/A.kt"))
    }

    @Test
    fun `changes in several modules are combined`() {
        assertEquals(
            setOf(":app", ":feature:alarms", ":feature:counter"),
            affected("feature/alarms/B.kt", "feature/counter/A.kt"),
        )
    }

    @Test
    fun `global files and directories affect everything`() {
        listOf(
            "build.gradle.kts",
            "gradle/libs.versions.toml",
            "build-logic/convention/src/main/kotlin/A.kt",
            "config/detekt/detekt.yml",
            ".github/workflows/ci.yml",
            ".editorconfig",
        ).forEach { file -> assertEquals(file, everything, affected(file)) }
    }

    @Test
    fun `markdown-only changes affect nothing`() {
        assertEquals(emptySet<String>(), affected("README.md", "feature/counter/README.md"))
    }

    @Test
    fun `files outside any module affect nothing`() {
        assertEquals(emptySet<String>(), affected(".github/CODEOWNERS", "scripts/new-module.sh"))
    }

    @Test
    fun `a directory prefix is not mistaken for a module`() {
        // "feature/counter-extras" must not match the "feature/counter" module.
        assertEquals(emptySet<String>(), affected("feature/counter-extras/A.kt"))
    }

    @Test
    fun `nested modules map to the most specific directory`() {
        val nested =
            listOf(
                ModuleInfo(":core:data", "core/data", emptySet()),
                ModuleInfo(":core:data:api", "core/data/api", emptySet()),
            )

        val result = AffectedModules.calculate(nested, listOf("core/data/api/A.kt")).modules

        assertEquals(setOf(":core:data:api"), result)
    }
}
