package io.github.ddiling.mydayboard.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

/**
 * Dependency injection with Hilt, for any Android module (the app and every feature).
 *
 * Why KSP and not kapt: KSP reads Kotlin directly instead of generating Java stubs first,
 * so it is faster. kapt is in maintenance mode and, with AGP 9 built-in Kotlin, would need the
 * separate `com.android.legacy-kapt` plugin.
 *
 * Why the Hilt Gradle plugin: it lets `@AndroidEntryPoint` classes extend their real base class
 * (bytecode rewrite) and aggregates the generated Hilt code of all modules in the app,
 * so a feature can define bindings without the app listing them.
 *
 * The compiler also runs on unit tests (`kspTest`), because `@HiltAndroidTest` classes
 * get their own generated component.
 *
 * Apply after mydayboard.android.application or mydayboard.android.library.
 */
class AndroidHiltConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.google.devtools.ksp")
            pluginManager.apply("com.google.dagger.hilt.android")

            dependencies {
                add("implementation", libs.library("hilt-android"))
                add("ksp", libs.library("hilt-compiler"))
                add("testImplementation", libs.library("hilt-android-testing"))
                add("kspTest", libs.library("hilt-compiler"))
            }
        }
    }
}
