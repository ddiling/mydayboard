package io.github.ddiling.mydayboard.buildlogic

import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.LibraryExtension
import io.github.takahirom.roborazzi.RoborazziExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.findByType

/**
 * Visual regression testing (the Android counterpart of Chromatic).
 *
 * Every @Preview in the module becomes a Robolectric screenshot test, no device
 * needed. Baselines live in <module>/src/test/screenshots and are committed.
 *
 *   ./gradlew recordRoborazziDebug    # write/refresh baselines
 *   ./gradlew verifyRoborazziDebug    # fail on any pixel diff (CI)
 *   ./gradlew compareRoborazziDebug   # write *_compare.png diffs without failing
 */
class AndroidScreenshotConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("mydayboard.android.compose")
            pluginManager.apply("io.github.takahirom.roborazzi")

            pluginManager.withPlugin("com.android.application") {
                extensions.configure<ApplicationExtension> {
                    testOptions.unitTests.isIncludeAndroidResources = true
                }
            }
            pluginManager.withPlugin("com.android.library") {
                extensions.configure<LibraryExtension> {
                    testOptions.unitTests.isIncludeAndroidResources = true
                }
            }

            extensions.configure<RoborazziExtension> {
                outputDir.set(layout.projectDirectory.dir("src/test/screenshots"))
                generateComposePreviewRobolectricTests {
                    enable.set(true)
                    includePrivatePreviews.set(true)
                    // Scan the module's own package for @Preview functions.
                    packages.set(provider { listOfNotNull(androidNamespace()) })
                }
            }

            dependencies {
                add("testImplementation", libs.library("junit"))
                add("testImplementation", libs.library("robolectric"))
                add("testImplementation", libs.library("roborazzi"))
                add("testImplementation", libs.library("roborazzi-compose"))
                add("testImplementation", libs.library("roborazzi-compose-preview-scanner-support"))
                add("testImplementation", libs.library("composable-preview-scanner"))
                add("testImplementation", libs.library("androidx-compose-ui-test-junit4"))
                add("debugImplementation", libs.library("androidx-compose-ui-test-manifest"))
            }

            // Runs the debug unit tests in verify mode: any pixel diff fails CI.
            addToCiTask(CiTasks.TEST, "verifyRoborazziDebug")
        }
    }

    private fun Project.androidNamespace(): String? =
        extensions.findByType<ApplicationExtension>()?.namespace
            ?: extensions.findByType<LibraryExtension>()?.namespace
}
