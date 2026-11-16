package io.github.ddiling.mydayboard.buildlogic

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.artifacts.ProjectDependency
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

    @TaskAction
    fun check() {
        val violations = ModuleRules.violations(modulePath.get(), projectDependencies.get())
        if (violations.isNotEmpty()) {
            throw GradleException(
                "Module dependency rules violated:\n" +
                    violations.joinToString("\n") { "  - $it" } +
                    "\nSee ModuleRules.kt in build-logic for the allowed directions.",
            )
        }
    }
}
