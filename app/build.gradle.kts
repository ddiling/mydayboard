plugins {
    alias(libs.plugins.mydayboard.android.application)
    alias(libs.plugins.mydayboard.android.compose)
    alias(libs.plugins.mydayboard.android.hilt)
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
    implementation(projects.core.data)
    implementation(projects.core.designsystem)
    implementation(projects.core.dashboard)
    implementation(projects.core.navigation)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.serialization.core)
    testImplementation(libs.androidx.navigation.testing)
}
