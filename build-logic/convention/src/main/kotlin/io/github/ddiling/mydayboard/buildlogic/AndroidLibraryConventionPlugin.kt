package io.github.ddiling.mydayboard.buildlogic

import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.android.library")
            pluginManager.apply("mydayboard.quality")

            extensions.configure<LibraryExtension> {
                configureAndroidCommon()

                defaultConfig {
                    configureAndroidDefaults()
                    consumerProguardFiles("consumer-rules.pro")
                }

                lint {
                    // The app module lints libraries via checkDependencies; this
                    // config applies when a library is linted on its own.
                    configureStrictLint(target, checkDependencies = false)
                }
            }

            configureKotlin()

            // Android Lint for libraries runs through the apps (checkDependencies),
            // and any change to a library marks its apps as affected.
            addToCiTask(CiTasks.TEST, "testDebugUnitTest")
        }
    }
}
