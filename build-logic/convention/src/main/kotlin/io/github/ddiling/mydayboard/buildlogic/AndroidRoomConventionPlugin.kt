package io.github.ddiling.mydayboard.buildlogic

import androidx.room.gradle.RoomExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

/**
 * A Room database with KSP, for the module that owns the database.
 *
 * Why the Room Gradle plugin: it passes the schema directory to the Room compiler as a
 * proper task input/output, so schema export works with the build cache and the exported
 * JSON lands in `schemas/`. Those files are committed: they are the history that future
 * migration tests (`room-testing`'s `MigrationTestHelper`) compare against.
 *
 * Why no `room-ktx`: since Room 2.7 its coroutine and Flow support lives in `room-runtime`,
 * and the 2.8 `room-ktx` artifact is empty.
 *
 * KSP, not kapt, for the same reasons as in [AndroidHiltConventionPlugin].
 *
 * Apply after mydayboard.android.library.
 */
class AndroidRoomConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.google.devtools.ksp")
            pluginManager.apply("androidx.room")

            extensions.configure<RoomExtension> {
                schemaDirectory("$projectDir/schemas")
            }

            dependencies {
                add("implementation", libs.library("androidx-room-runtime"))
                add("ksp", libs.library("androidx-room-compiler"))
                add("testImplementation", libs.library("androidx-room-testing"))
            }
        }
    }
}
