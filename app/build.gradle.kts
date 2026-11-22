plugins {
    alias(libs.plugins.mydayboard.android.application)
    alias(libs.plugins.mydayboard.android.compose)
    alias(libs.plugins.mydayboard.android.hilt)
    // Baselines for the previews in :app (the header and the settings placeholder).
    alias(libs.plugins.mydayboard.android.screenshot)
    // For DashboardDestination, the one route :app owns itself.
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "io.github.ddiling.mydayboard"
    defaultConfig {
        applicationId = "io.github.ddiling.mydayboard"
    }
}

dependencies {
    implementation(projects.feature.trackers)
    implementation(projects.feature.tasks)
    implementation(projects.core.data)
    implementation(projects.core.designsystem)
    implementation(projects.core.dashboard)
    implementation(projects.core.navigation)

    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.serialization.core)
    testImplementation(projects.core.testing)
    testImplementation(libs.androidx.navigation.testing)
}

roborazzi {
    generateComposePreviewRobolectricTests {
        // The default scans the whole namespace, which would re-record every library module's previews here.
        packages.set(listOf("io.github.ddiling.mydayboard.previews"))
    }
}
