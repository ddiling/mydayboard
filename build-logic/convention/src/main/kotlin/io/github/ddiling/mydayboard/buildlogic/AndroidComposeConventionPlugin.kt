package io.github.ddiling.mydayboard.buildlogic

import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.jetbrains.kotlin.compose.compiler.gradle.ComposeCompilerGradlePluginExtension

/** Apply after mydayboard.android.application or mydayboard.android.library. */
class AndroidComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("org.jetbrains.kotlin.plugin.compose")

            pluginManager.withPlugin("com.android.application") {
                extensions.configure<ApplicationExtension> { buildFeatures { compose = true } }
            }
            pluginManager.withPlugin("com.android.library") {
                extensions.configure<LibraryExtension> { buildFeatures { compose = true } }
            }

            extensions.configure<ComposeCompilerGradlePluginExtension> {
                // Classes from other modules/libraries that Compose should treat as stable.
                stabilityConfigurationFiles.add(
                    rootProject.layout.projectDirectory.file("config/compose/stability.conf"),
                )
                // ./gradlew assembleRelease -PcomposeCompilerReports=true
                if (providers.gradleProperty("composeCompilerReports").orNull == "true") {
                    reportsDestination.set(layout.buildDirectory.dir("compose-compiler/reports"))
                    metricsDestination.set(layout.buildDirectory.dir("compose-compiler/metrics"))
                }
            }

            dependencies {
                val bom = platform(libs.library("androidx-compose-bom"))
                add("implementation", bom)
                add("testImplementation", bom)
                add("androidTestImplementation", bom)
                add("implementation", libs.library("androidx-compose-ui-tooling-preview"))
                add("debugImplementation", libs.library("androidx-compose-ui-tooling"))
            }
        }
    }
}
