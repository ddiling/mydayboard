package io.github.ddiling.mydayboard.buildlogic

import kotlinx.kover.gradle.plugin.dsl.KoverProjectExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

/**
 * A feature module = Android library + Compose + screenshot tests + Hilt, wired to
 * the shared core modules. A new feature's build script is just a namespace.
 *
 * Coverage: the ViewModels are the logic of a feature, so they get a minimum
 * ([VIEW_MODEL_MIN_COVERAGE]%) in their own Kover variant. Composables are reported
 * (merged report) but never gated: screenshot tests prove they render, not that they behave.
 */
class AndroidFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("mydayboard.android.library")
            pluginManager.apply("mydayboard.android.compose")
            pluginManager.apply("mydayboard.android.screenshot")
            // Every feature gets its ViewModels from Hilt (hiltViewModel()), never from hand-written factories.
            pluginManager.apply("mydayboard.android.hilt")

            extensions.configure<KoverProjectExtension> {
                currentProject { createVariant(VIEW_MODEL_VARIANT) { add("debug") } }
                reports {
                    variant(VIEW_MODEL_VARIANT) {
                        filters { includes { classes("*ViewModel") } }
                        verify { rule { minBound(VIEW_MODEL_MIN_COVERAGE) } }
                    }
                }
            }
            addToCiTask(CiTasks.TEST, "koverVerifyViewModels")

            dependencies {
                add("implementation", project(":core:designsystem"))
                add("implementation", project(":core:domain"))
                add("implementation", libs.library("androidx-lifecycle-runtime-compose"))
                add("implementation", libs.library("androidx-lifecycle-viewmodel-compose"))
                add("implementation", libs.library("androidx-hilt-lifecycle-viewmodel-compose"))
                add("testImplementation", libs.library("junit"))
            }
        }
    }

    private companion object {
        /** Kover derives the task name `koverVerifyViewModels` from this. */
        const val VIEW_MODEL_VARIANT = "viewModels"
        const val VIEW_MODEL_MIN_COVERAGE = 80
    }
}
