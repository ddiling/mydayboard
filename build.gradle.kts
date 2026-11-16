// Plugins are declared here with `apply false` so they share ONE classloader.
// Convention plugins in build-logic/ depend on them as compileOnly and apply
// them by id, which is why they must all be on the root classpath.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    // Pins the Kotlin Gradle Plugin version that AGP's built-in Kotlin uses.
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.detekt) apply false
    alias(libs.plugins.roborazzi) apply false
    alias(libs.plugins.spotless)
    // Merged coverage report (:koverHtmlReport); every module applies it via mydayboard.quality.
    alias(libs.plugins.kover)
    // :affectedStaticAnalysis, :affectedTest, :affectedAssemble, :printAffectedModules
    alias(libs.plugins.mydayboard.root)
}

// Formatting for files that don't belong to any module.
spotless {
    kotlinGradle {
        target("*.gradle.kts", "build-logic/**/*.gradle.kts")
        targetExclude("**/build/**")
        ktlint(libs.versions.ktlint.get())
    }
    kotlin {
        target("build-logic/**/*.kt")
        targetExclude("**/build/**")
        ktlint(libs.versions.ktlint.get())
    }
    format("misc") {
        target("*.md", ".gitignore", ".github/**/*.yml", "app/maestro/**/*.yaml", "config/**/*.yml")
        trimTrailingWhitespace()
        endWithNewline()
    }
}
