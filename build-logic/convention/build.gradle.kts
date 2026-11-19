import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    `kotlin-dsl`
}

group = "io.github.ddiling.mydayboard.buildlogic"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}

dependencies {
    // compileOnly: the real plugin jars come from the root project's classpath
    // (see `apply false` in the root build.gradle.kts).
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.compose.gradlePlugin)
    compileOnly(libs.detekt.gradlePlugin)
    compileOnly(libs.spotless.gradlePlugin)
    compileOnly(libs.kover.gradlePlugin)
    compileOnly(libs.roborazzi.gradlePlugin)
    compileOnly(libs.ksp.gradlePlugin)
    compileOnly(libs.hilt.gradlePlugin)

    testImplementation(libs.junit)
}

tasks.validatePlugins {
    enableStricterValidation = true
    failOnWarning = true
}

gradlePlugin {
    plugins {
        register("androidApplication") {
            id =
                libs.plugins.mydayboard.android.application
                    .get()
                    .pluginId
            implementationClass = "io.github.ddiling.mydayboard.buildlogic.AndroidApplicationConventionPlugin"
        }
        register("androidLibrary") {
            id =
                libs.plugins.mydayboard.android.library
                    .get()
                    .pluginId
            implementationClass = "io.github.ddiling.mydayboard.buildlogic.AndroidLibraryConventionPlugin"
        }
        register("androidCompose") {
            id =
                libs.plugins.mydayboard.android.compose
                    .get()
                    .pluginId
            implementationClass = "io.github.ddiling.mydayboard.buildlogic.AndroidComposeConventionPlugin"
        }
        register("androidFeature") {
            id =
                libs.plugins.mydayboard.android.feature
                    .get()
                    .pluginId
            implementationClass = "io.github.ddiling.mydayboard.buildlogic.AndroidFeatureConventionPlugin"
        }
        register("androidHilt") {
            id =
                libs.plugins.mydayboard.android.hilt
                    .get()
                    .pluginId
            implementationClass = "io.github.ddiling.mydayboard.buildlogic.AndroidHiltConventionPlugin"
        }
        register("root") {
            id =
                libs.plugins.mydayboard.root
                    .get()
                    .pluginId
            implementationClass = "io.github.ddiling.mydayboard.buildlogic.RootConventionPlugin"
        }
        register("androidScreenshot") {
            id =
                libs.plugins.mydayboard.android.screenshot
                    .get()
                    .pluginId
            implementationClass = "io.github.ddiling.mydayboard.buildlogic.AndroidScreenshotConventionPlugin"
        }
        register("jvmLibrary") {
            id =
                libs.plugins.mydayboard.jvm.library
                    .get()
                    .pluginId
            implementationClass = "io.github.ddiling.mydayboard.buildlogic.JvmLibraryConventionPlugin"
        }
        register("quality") {
            id =
                libs.plugins.mydayboard.quality
                    .get()
                    .pluginId
            implementationClass = "io.github.ddiling.mydayboard.buildlogic.QualityConventionPlugin"
        }
    }
}
