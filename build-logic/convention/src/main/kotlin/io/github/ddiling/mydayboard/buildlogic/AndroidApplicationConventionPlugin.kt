package io.github.ddiling.mydayboard.buildlogic

import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

/**
 * Everything an app module needs except its namespace, applicationId and
 * dependencies, so a new app is ~15 lines of build script.
 *
 * Versioning and signing come from environment variables set by the release
 * workflow; local builds get safe defaults and an unsigned release build.
 */
class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.android.application")
            pluginManager.apply("mydayboard.quality")

            val keystorePath = providers.environmentVariable("ANDROID_KEYSTORE_PATH")

            extensions.configure<ApplicationExtension> {
                configureAndroidCommon()

                defaultConfig {
                    configureAndroidDefaults()
                    targetSdk { version = release(ProjectConfig.TARGET_SDK) }
                    versionCode = providers.environmentVariable("VERSION_CODE").map(String::toInt).getOrElse(1)
                    versionName = providers.environmentVariable("VERSION_NAME").getOrElse("0.1.0-dev")
                }

                if (keystorePath.isPresent) {
                    signingConfigs.create("release") {
                        storeFile = file(keystorePath.get())
                        storePassword = providers.environmentVariable("ANDROID_KEYSTORE_PASSWORD").get()
                        keyAlias = providers.environmentVariable("ANDROID_KEY_ALIAS").get()
                        keyPassword = providers.environmentVariable("ANDROID_KEY_PASSWORD").get()
                    }
                }

                buildTypes {
                    getByName("debug") {
                        // Debug and release builds can be installed side by side.
                        applicationIdSuffix = ".debug"
                    }
                    getByName("release") {
                        isMinifyEnabled = true
                        isShrinkResources = true
                        proguardFiles(
                            getDefaultProguardFile("proguard-android-optimize.txt"),
                            "proguard-rules.pro",
                        )
                        // Unsigned when no keystore is configured (PR builds, local builds).
                        signingConfigs.findByName("release")?.let { signingConfig = it }
                    }
                }

                // Robolectric tests of the whole app (e.g. MainActivityTest) read the merged
                // resources, so strings render as on a device.
                testOptions.unitTests.isIncludeAndroidResources = true

                packaging {
                    resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
                }

                lint {
                    // Lints this app and every library module it depends on in one pass.
                    configureStrictLint(target, checkDependencies = true)
                }
            }

            configureKotlin()

            // App-level tests start real activities on the JVM with Robolectric, no emulator needed.
            dependencies {
                add("testImplementation", libs.library("junit"))
                add("testImplementation", libs.library("robolectric"))
            }
            // The Compose test APIs are versioned by the Compose BOM, which the compose convention adds.
            pluginManager.withPlugin("org.jetbrains.kotlin.plugin.compose") {
                dependencies { add("testImplementation", libs.library("androidx-compose-ui-test-junit4")) }
            }

            addToCiTask(CiTasks.STATIC_ANALYSIS, "lintDebug")
            addToCiTask(CiTasks.TEST, "testDebugUnitTest")
            addToCiTask(CiTasks.ASSEMBLE, "assembleDebug", "assembleRelease")
        }
    }
}
