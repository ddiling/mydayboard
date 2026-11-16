package io.github.ddiling.mydayboard.buildlogic

import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.artifacts.MinimalExternalModuleDependency
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

/** Single source of truth for SDK levels and the Java/Kotlin bytecode target. */
internal object ProjectConfig {
    const val COMPILE_SDK = 37
    const val TARGET_SDK = 37
    const val MIN_SDK = 26
    const val JAVA_RELEASE = 17
    val JAVA_VERSION: JavaVersion = JavaVersion.VERSION_17
    val JVM_TARGET: JvmTarget = JvmTarget.JVM_17
}

internal val Project.libs: VersionCatalog
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")

internal fun VersionCatalog.library(alias: String): Provider<MinimalExternalModuleDependency> = findLibrary(alias).get()

internal fun VersionCatalog.version(alias: String): String = findVersion(alias).get().requiredVersion

/**
 * Kotlin compiler settings shared by every module (Android and pure JVM).
 * With AGP 9 built-in Kotlin, Android modules also compile through KotlinJvmCompile.
 */
internal fun Project.configureKotlin() {
    val warningsAsErrors =
        providers
            .gradleProperty("warningsAsErrors")
            .map(String::toBoolean)
            .orElse(false)

    tasks.withType<KotlinJvmCompile>().configureEach {
        compilerOptions.jvmTarget.set(ProjectConfig.JVM_TARGET)
        compilerOptions.allWarningsAsErrors.set(warningsAsErrors)
        // Fail on deprecated language constructs early instead of at the next Kotlin upgrade.
        compilerOptions.progressiveMode.set(true)
    }
}
