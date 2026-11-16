package io.github.ddiling.mydayboard.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.withType

/** Pure Kotlin/JVM module: no Android dependency, fastest to build and test. */
class JvmLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("org.jetbrains.kotlin.jvm")
            pluginManager.apply("mydayboard.quality")

            tasks.withType<JavaCompile>().configureEach {
                options.release.set(ProjectConfig.JAVA_RELEASE)
            }
            configureKotlin()

            dependencies {
                add("testImplementation", libs.library("junit"))
            }

            addToCiTask(CiTasks.TEST, "test")
        }
    }
}
