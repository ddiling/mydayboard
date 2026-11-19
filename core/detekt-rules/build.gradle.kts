// Custom detekt rules. Lives under core/ because the module rules only know app, feature/ and core/;
// nothing depends on it at runtime, it is only handed to detekt (see QualityConventionPlugin).
plugins {
    alias(libs.plugins.mydayboard.jvm.library)
}

dependencies {
    // compileOnly: detekt supplies its own API at runtime, the rule must not bring a second copy.
    compileOnly(libs.detekt.api)

    testImplementation(libs.detekt.api)
    testImplementation(libs.detekt.test)
}
