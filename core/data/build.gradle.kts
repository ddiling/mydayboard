plugins {
    alias(libs.plugins.mydayboard.android.library)
    alias(libs.plugins.mydayboard.android.hilt)
    // Applied by the conventions already; declared here to get the `kover { }` accessor.
    alias(libs.plugins.kover)
}

android {
    namespace = "io.github.ddiling.mydayboard.core.data"
}

// Repositories are logic, so they get the same coverage floor as the domain.
kover {
    reports {
        verify {
            rule {
                minBound(80)
            }
        }
    }
}

dependencies {
    api(projects.core.domain)
    implementation(projects.core.database)

    // Not a cycle: :core:testing uses :core:domain only; these tests run its contract against Room.
    testImplementation(projects.core.testing)
    testImplementation(libs.androidx.room.runtime)
    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.kotlinx.coroutines.test)
}
