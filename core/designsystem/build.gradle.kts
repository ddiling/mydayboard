plugins {
    alias(libs.plugins.mydayboard.android.library)
    alias(libs.plugins.mydayboard.android.compose)
    alias(libs.plugins.mydayboard.android.screenshot)
}

android {
    namespace = "io.github.ddiling.mydayboard.core.designsystem"
}

dependencies {
    api(libs.androidx.compose.material3)
    api(libs.androidx.compose.foundation)
    api(libs.androidx.compose.ui)
}
