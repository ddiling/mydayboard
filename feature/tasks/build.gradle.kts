plugins {
    alias(libs.plugins.mydayboard.android.feature)
}

android {
    namespace = "io.github.ddiling.mydayboard.feature.tasks"
}

dependencies {
    implementation(projects.core.dashboard)
    implementation(libs.androidx.compose.material.icons.core)
    testImplementation(projects.core.testing)
}
