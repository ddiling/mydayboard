package io.github.ddiling.mydayboard.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Applied to the root project only. Registers the "affected" CI entry points:
 *
 *   ./gradlew :affectedTest -PaffectedBase=origin/main   # only what this branch changed
 *   ./gradlew :affectedTest                              # everything
 *   ./gradlew -q :printAffectedModules -PaffectedBase=origin/main
 *
 * Always call them with the leading ':' so Gradle doesn't also look for
 * same-named tasks in subprojects.
 */
class RootConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            check(this == rootProject) { "mydayboard.root must be applied to the root project only." }

            // Computed once, after all modules are configured.
            val affectedModules by lazy { AffectedModules.compute(this) }
            val affected = provider { affectedModules }

            CiTasks.affectedTasks.forEach { (rootTask, moduleTask) ->
                tasks.register(rootTask) {
                    group = "ci"
                    description = "Runs $moduleTask in modules affected by the change (all without -PaffectedBase)."
                    dependsOn(affected.map { modules -> modules.map { "$it:$moduleTask" } })
                }
            }
            // Root-level files (build scripts, markdown, workflows) are formatted by the root Spotless.
            tasks.named("affectedStaticAnalysis") { dependsOn(":spotlessCheck") }

            // One merged coverage report across all modules: ./gradlew :koverHtmlReport
            subprojects.forEach { module ->
                if (module.buildFile.exists()) {
                    dependencies.add(
                        "kover",
                        dependencies.project(
                            mapOf("path" to module.path),
                        ),
                    )
                }
            }

            // Roborazzi only renders the previews when a record/verify task is in the task graph.
            // Running Kover's report on its own executes the screenshot tests as no-ops and under-reports
            // UI coverage, so this task pairs the report with ciTest (which includes verifyRoborazziDebug).
            tasks.register("coverageReport") {
                group = "ci"
                description = "Runs all tests incl. screenshot verification, then builds the merged coverage report."
                dependsOn(
                    provider { subprojects.filter { it.buildFile.exists() }.map { "${it.path}:${CiTasks.TEST}" } },
                )
                dependsOn("koverHtmlReport")
            }

            tasks.register("printAffectedModules") {
                group = "ci"
                description = "Prints the modules affected by the change, one per line."
                val modules = affected
                doLast { println(modules.get().joinToString("\n").ifEmpty { "(none)" }) }
            }
        }
    }
}
