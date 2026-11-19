// Shared test fixtures: a hand-driven clock, predictable ids, a Main dispatcher rule and named zones.
// Other modules only ever add it as `testImplementation(projects.core.testing)`, never to production code.
plugins {
    alias(libs.plugins.mydayboard.jvm.library)
}

dependencies {
    api(projects.core.domain)
    // `api`, so that one testImplementation line gives a module the whole test toolkit.
    api(libs.junit)
    api(libs.kotlinx.coroutines.test)
    api(libs.turbine)
}
