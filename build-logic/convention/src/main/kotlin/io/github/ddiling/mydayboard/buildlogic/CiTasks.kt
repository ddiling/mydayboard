package io.github.ddiling.mydayboard.buildlogic

import org.gradle.api.Project

/**
 * Every module gets three lifecycle tasks. Convention plugins attach the right
 * work to them for each module type, so CI never needs to know module types:
 *
 *   ciStaticAnalysis  formatting, detekt, module rules (+ Android Lint on apps)
 *   ciTest            unit tests (+ screenshot verification where applied)
 *   ciAssemble        debug + release builds (apps only)
 *
 * `./gradlew ciTest` runs it in every module. The root tasks
 * `:affectedStaticAnalysis`, `:affectedTest` and `:affectedAssemble` run it
 * only in modules affected by the current change (see AffectedModules.kt).
 */
internal object CiTasks {
    const val STATIC_ANALYSIS = "ciStaticAnalysis"
    const val TEST = "ciTest"
    const val ASSEMBLE = "ciAssemble"

    /** Root task name -> module task name. */
    val affectedTasks: Map<String, String> =
        mapOf(
            "affectedStaticAnalysis" to STATIC_ANALYSIS,
            "affectedTest" to TEST,
            "affectedAssemble" to ASSEMBLE,
        )
}

internal fun Project.registerCiTasks() {
    tasks.register(CiTasks.STATIC_ANALYSIS) {
        group = "ci"
        description = "Formatting, static analysis and module rules for this module."
    }
    tasks.register(CiTasks.TEST) {
        group = "ci"
        description = "Unit and screenshot tests for this module."
    }
    tasks.register(CiTasks.ASSEMBLE) {
        group = "ci"
        description = "Builds this module's shippable outputs (apps only)."
    }
}

/** Adds work to one of this module's CI lifecycle tasks. Task names resolve lazily. */
internal fun Project.addToCiTask(
    ciTask: String,
    vararg taskNames: String,
) {
    tasks.named(ciTask) { dependsOn(*taskNames) }
}
