package io.github.ddiling.mydayboard.buildlogic

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault

/**
 * The project's architecture rules, enforced on every build:
 *
 *   :app      -> feature, core
 *   feature   -> core
 *   core      -> core
 *
 * (Written without wildcards on purpose: a slash-star sequence inside a Kotlin
 * comment opens a nested comment and silently comments out the code below.)
 *
 * So: nothing depends on :app, no feature depends on another feature, and core
 * never depends on features. Code that two features need goes to core. The
 * dashboard lives in :app because :app is the only layer allowed to know every feature.
 */
internal object ModuleRules {
    /** The application module is a single module, so it is matched exactly, not by prefix. */
    private const val APP = ":app"
    private const val FEATURE = ":feature:"
    private const val CORE = ":core:"

    private val allowedDependencies: Map<String, Set<String>> =
        mapOf(
            APP to setOf(FEATURE, CORE),
            FEATURE to setOf(CORE),
            CORE to setOf(CORE),
        )

    private fun layerOf(path: String): String? =
        if (path == APP) {
            APP
        } else {
            listOf(FEATURE, CORE).firstOrNull { path.startsWith(it) }
        }

    private fun describe(layer: String) = if (layer == APP) "app" else layer.trim(':') + "/*"

    private const val LAYERS_HINT = "app, feature/ or core/"

    private const val TESTING = ":core:testing"

    /** Test configurations: `test…`, `androidTest…` and processors such as `kspTest`. */
    private val testConfiguration = Regex("^(ksp|kapt)?(test|androidTest)", RegexOption.IGNORE_CASE)

    /**
     * `:core:testing` bundles JUnit, fakes and a hand-driven clock, so shipping it in a production
     * configuration would put test code in the app. [configurations] maps each configuration of the
     * module to the project paths it declares.
     */
    fun testingToolkitViolations(
        module: String,
        configurations: Map<String, Set<String>>,
    ): List<String> =
        configurations
            .filter { (name, paths) -> TESTING in paths && !testConfiguration.containsMatchIn(name) }
            .keys
            .sorted()
            .map { "$module declares $TESTING in `$it`; use it only in test configurations, e.g. testImplementation." }

    fun violations(
        module: String,
        dependencies: Collection<String>,
    ): List<String> {
        val layer =
            layerOf(module)
                ?: return listOf("$module must be :app or live under feature/ or core/.")
        val allowed = allowedDependencies.getValue(layer)

        return dependencies
            .filter { it != module }
            .sorted()
            .mapNotNull { dependency ->
                val dependencyLayer = layerOf(dependency)
                when {
                    dependencyLayer == null -> {
                        "$module -> $dependency: $dependency must be one of $LAYERS_HINT."
                    }

                    dependencyLayer !in allowed -> {
                        "$module -> $dependency: ${describe(layer)} may only depend on " +
                            allowed.joinToString(" and ") { describe(it) } + "."
                    }

                    else -> {
                        null
                    }
                }
            }
    }
}

/** Project paths declared per configuration of this project. */
internal fun Project.declaredProjectDependenciesByConfiguration(): Map<String, Set<String>> {
    val self = path
    return configurations.associate { configuration ->
        val paths = configuration.dependencies.withType(ProjectDependency::class.java).map { it.path }
        configuration.name to paths.filterTo(sortedSetOf()) { it != self }
    }
}

/** Paths of all project dependencies declared in any configuration of this project. */
internal fun Project.declaredProjectDependencyPaths(): Set<String> =
    configurations
        .flatMap { configuration ->
            configuration.dependencies.withType(ProjectDependency::class.java).map { it.path }
        }.filterTo(sortedSetOf()) { it != path }

@DisableCachingByDefault(because = "Fast check without outputs")
abstract class CheckModuleDependenciesTask : DefaultTask() {
    @get:Input
    abstract val modulePath: Property<String>

    @get:Input
    abstract val projectDependencies: SetProperty<String>

    @get:Input
    abstract val dependenciesByConfiguration: MapProperty<String, Set<String>>

    @TaskAction
    fun check() {
        val module = modulePath.get()
        val violations =
            ModuleRules.violations(module, projectDependencies.get()) +
                ModuleRules.testingToolkitViolations(module, dependenciesByConfiguration.get())
        if (violations.isNotEmpty()) {
            throw GradleException(
                "Module dependency rules violated:\n" +
                    violations.joinToString("\n") { "  - $it" } +
                    "\nSee ModuleRules.kt in build-logic for the allowed directions.",
            )
        }
    }
}
