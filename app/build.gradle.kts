plugins {
    alias(libs.plugins.mydayboard.android.application)
    alias(libs.plugins.mydayboard.android.compose)
    alias(libs.plugins.mydayboard.android.hilt)
}

android {
    namespace = "io.github.ddiling.mydayboard"
    defaultConfig {
        applicationId = "io.github.ddiling.mydayboard"
    }
}

dependencies {
    implementation(projects.feature.counter)
    implementation(projects.core.designsystem)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    // Only the NavHost lives here; routes are defined by the features. The app declares no routes
    // itself, so it needs the library but not the serialization compiler plugin.
    implementation(libs.androidx.navigation.compose)
    testImplementation(libs.androidx.navigation.testing)
}
