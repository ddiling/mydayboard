package io.github.ddiling.mydayboard.buildlogic

import com.diffplug.gradle.spotless.SpotlessExtension
import dev.detekt.gradle.extensions.DetektExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.register

/**
 * Applied to every module by the other conventions:
 *
 * - Spotless/ktlint: formatting & style (auto-fixable: ./gradlew spotlessApply)
 * - Detekt: code smells, complexity, potential bugs (config/detekt/detekt.yml)
 * - Kover: coverage per module, merged at the root (:koverHtmlReport). Minimums are set per module,
 *   see core/domain/build.gradle.kts
 * - checkModuleDependencies: architecture rules (ModuleRules.kt)
 * - CI lifecycle tasks (CiTasks.kt)
 *
 * All checks are wired into `./gradlew check`.
 */
class QualityConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("dev.detekt")
            pluginManager.apply("com.diffplug.spotless")
            pluginManager.apply("org.jetbrains.kotlinx.kover")

            extensions.configure<DetektExtension> {
                buildUponDefaultConfig.set(true)
                parallel.set(true)
                config.setFrom(rootProject.file("config/detekt/detekt.yml"))
                // Park pre-existing findings with: ./gradlew detektBaseline
                baseline.set(layout.projectDirectory.file("detekt-baseline.xml"))
            }

            // The project's own rules (NoAnyType, ...). The detekt task analyses src/main and src/test,
            // so the rules cover test sources too.
            dependencies {
                add("detektPlugins", project(":core:detekt-rules"))
            }

            val ktlintVersion = libs.version("ktlint")
            extensions.configure<SpotlessExtension> {
                kotlin {
                    target("src/**/*.kt")
                    targetExclude("**/build/**")
                    ktlint(ktlintVersion)
                }
                kotlinGradle {
                    target("*.gradle.kts")
                    ktlint(ktlintVersion)
                }
                format("xml") {
                    target("src/**/*.xml")
                    targetExclude("**/build/**")
                    trimTrailingWhitespace()
                    endWithNewline()
                }
            }

            val project = this
            val checkModuleDependencies =
                tasks.register<CheckModuleDependenciesTask>("checkModuleDependencies") {
                    group = "verification"
                    description = "Fails if this module depends on a module it must not (see ModuleRules.kt)."
                    modulePath.set(project.path)
                    projectDependencies.set(project.provider { project.declaredProjectDependencyPaths() })
                    dependenciesByConfiguration.set(
                        project.provider { project.declaredProjectDependenciesByConfiguration() },
                    )
                }
            tasks.matching { it.name == "check" }.configureEach { dependsOn(checkModuleDependencies) }

            registerCiTasks()
            // detektMain/detektTest run with type resolution, which rules such as UnusedImport need;
            // plain `detekt` does not, so those rules would silently never fire.
            addToCiTask(
                CiTasks.STATIC_ANALYSIS,
                "spotlessCheck",
                "detekt",
                "detektMain",
                "detektTest",
                "checkModuleDependencies",
            )
            // Runs the tests under coverage and fails if a module-specific minimum is not met.
            addToCiTask(CiTasks.TEST, "koverVerify")
        }
    }
}
