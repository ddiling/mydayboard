plugins {
    alias(libs.plugins.mydayboard.android.feature)
}

android {
    namespace = "io.github.ddiling.mydayboard.feature.trackers"
}

dependencies {
    testImplementation(projects.core.testing)
}
