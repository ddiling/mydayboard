plugins {
    alias(libs.plugins.mydayboard.jvm.library)
    // Applied by the conventions already; declared here to get the `kover { }` accessor.
    alias(libs.plugins.kover)
}

dependencies {
    // Only the standard @Inject annotation: Hilt can build the use cases, but this module stays free of
    // Android and Dagger.
    implementation(libs.javax.inject)
}

// Business logic is the one place where a coverage floor is worth enforcing.
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
    // `api`: minuteTicker returns a Flow, so every caller needs coroutines on its compile classpath too.
    api(libs.kotlinx.coroutines.core)

    // Not a cycle: :core:testing uses this module's main code; only the tests here use :core:testing.
    testImplementation(projects.core.testing)
}
