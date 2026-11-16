pluginManagement {
    includeBuild("build-logic")
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
    }
}

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

rootProject.name = "mydayboard"

// Layers (enforced by checkModuleDependencies, see build-logic/ModuleRules.kt):
//   :app      -> may depend on feature/* and core/*
//   feature/* -> may depend on core/* only
//   core/*    -> may depend on core/* only

// The application: a thin shell that owns navigation and the dashboard.
include(":app")

// Features: one module per area (trackers, alarms, ...), each a screen or flow.
include(":feature:counter")

// Core: shared foundations.
include(":core:domain")
include(":core:designsystem")
