plugins {
    alias(libs.plugins.mydayboard.android.library)
    alias(libs.plugins.mydayboard.android.hilt)
    alias(libs.plugins.mydayboard.android.room)
}

android {
    namespace = "io.github.ddiling.mydayboard.core.database"
}

dependencies {
    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
}
