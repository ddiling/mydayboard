package io.github.ddiling.mydayboard.buildlogic

import org.gradle.api.Project

/** A module as the affected-module calculation sees it: no Gradle types, easy to fake in tests. */
internal data class ModuleInfo(
    val path: String,
    /** Project directory relative to the repository root, e.g. `feature/trackers`. */
    val directory: String,
    /** Gradle paths of the project dependencies this module declares. */
    val dependencies: Set<String>,
)

/** [modules] to check, and why (used for the log line and the CI summary). */
internal data class AffectedResult(
    val modules: Set<String>,
    val reason: String,
)

/**
 * Works out which modules a change touches, so PR builds skip everything else.
 *
 * 1. `git diff --name-only <affectedBase>...HEAD` lists changed files.
 * 2. Each file is mapped to the module whose directory contains it.
 * 3. Every module that depends on a changed module (transitively) is added.
 *    A change in core/designsystem therefore affects every feature and app.
 *
 * Falls back to ALL modules when no base is given (main builds, local runs),
 * when git fails, or when a file that influences every module changed.
 *
 * [calculate] is a pure function (unit-tested); the rest of this object only
 * gathers its inputs from Gradle and git.
 */
internal object AffectedModules {
    /** Root files that change how every module builds. */
    private val globalFiles =
        setOf(
            "build.gradle.kts",
            "settings.gradle.kts",
            "gradle.properties",
            "gradlew",
            "gradlew.bat",
            ".editorconfig",
        )

    /** Directories that change how every module builds or is checked. */
    private val globalDirectories = listOf("build-logic/", "gradle/", "config/", ".github/workflows/")

    /** Files that never affect a build. */
    private val ignoredSuffixes = listOf(".md")

    /**
     * @param changedFiles repo-relative paths with `/` separators, or null when unknown
     *   (no base given, or git failed), which means "everything".
     */
    fun calculate(
        modules: Collection<ModuleInfo>,
        changedFiles: List<String>?,
    ): AffectedResult {
        val allModules = modules.mapTo(sortedSetOf()) { it.path }
        if (changedFiles == null) {
            return AffectedResult(allModules, "all (no -PaffectedBase, or git diff failed)")
        }

        val relevantFiles = changedFiles.filterNot { file -> ignoredSuffixes.any(file::endsWith) }
        val globalChange =
            relevantFiles.firstOrNull { file ->
                file in globalFiles || globalDirectories.any(file::startsWith)
            }
        if (globalChange != null) {
            return AffectedResult(allModules, "all ($globalChange affects every module)")
        }

        val directlyChanged =
            relevantFiles.mapNotNullTo(mutableSetOf()) { file ->
                modules
                    .filter { module -> file.startsWith(module.directory.trimEnd('/') + "/") }
                    .maxByOrNull { module -> module.directory.length }
                    ?.path
            }

        val dependents = mutableMapOf<String, MutableSet<String>>()
        modules.forEach { module ->
            module.dependencies.forEach { dependency ->
                dependents.getOrPut(dependency) { mutableSetOf() } += module.path
            }
        }

        val affected = directlyChanged.toMutableSet()
        val queue = ArrayDeque(directlyChanged)
        while (queue.isNotEmpty()) {
            dependents[queue.removeFirst()].orEmpty().forEach { dependent ->
                if (affected.add(dependent)) queue += dependent
            }
        }
        return AffectedResult(affected.toSortedSet(), "changed modules plus their dependents")
    }

    fun compute(root: Project): Set<String> {
        val modules =
            root.subprojects
                .filter { it.buildFile.exists() }
                .map { module ->
                    ModuleInfo(
                        path = module.path,
                        directory = module.projectDir.relativeTo(root.projectDir).invariantSeparatorsPath,
                        dependencies = module.declaredProjectDependencyPaths(),
                    )
                }
        val result = calculate(modules, root.changedFiles())
        root.logger.lifecycle("Affected modules: ${result.reason}")
        return result.modules
    }

    private fun Project.changedFiles(): List<String>? {
        val base =
            providers
                .gradleProperty("affectedBase")
                .orNull
                ?.trim()
                .orEmpty()
        if (base.isEmpty()) return null

        val diff =
            providers.exec {
                commandLine("git", "diff", "--name-only", "$base...HEAD")
                isIgnoreExitValue = true
            }
        if (diff.result.get().exitValue != 0) {
            logger.warn("git diff against '$base' failed; treating all modules as affected.")
            return null
        }
        return diff.standardOutput.asText
            .get()
            .lineSequence()
            .map(String::trim)
            .filter(String::isNotEmpty)
            .toList()
    }
}
