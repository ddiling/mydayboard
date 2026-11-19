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
}
