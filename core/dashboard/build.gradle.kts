plugins {
    alias(libs.plugins.mydayboard.android.library)
    alias(libs.plugins.mydayboard.android.compose)
}

android {
    namespace = "io.github.ddiling.mydayboard.core.dashboard"
}

dependencies {
    // `api`: DashboardCard.Content exposes Compose and Material types (Modifier, SnackbarHostState).
    api(projects.core.designsystem)
    // `api`: DashboardHost.navigate exposes Destination.
    api(projects.core.navigation)

    testImplementation(libs.junit)
}
