package io.github.ddiling.mydayboard.buildlogic

import com.android.build.api.dsl.Lint
import org.gradle.api.Project

/**
 * Strict Android Lint: every warning fails the build. Noisy or "version is
 * outdated" checks are switched off in config/lint/lint.xml (Dependabot handles
 * those). Pre-existing issues can be parked in <module>/lint-baseline.xml via
 * `./gradlew updateLintBaseline`.
 */
internal fun Lint.configureStrictLint(
    project: Project,
    checkDependencies: Boolean,
) {
    abortOnError = true
    warningsAsErrors = true
    checkReleaseBuilds = true
    // On the app module this also lints all library modules in one pass.
    this.checkDependencies = checkDependencies
    lintConfig = project.rootProject.file("config/lint/lint.xml")
    baseline = project.file("lint-baseline.xml")
    htmlReport = true
    sarifReport = true
    xmlReport = false
}
