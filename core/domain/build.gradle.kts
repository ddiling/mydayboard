plugins {
    alias(libs.plugins.mydayboard.jvm.library)
    // Applied by the conventions already; declared here to get the `kover { }` accessor.
    alias(libs.plugins.kover)
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
