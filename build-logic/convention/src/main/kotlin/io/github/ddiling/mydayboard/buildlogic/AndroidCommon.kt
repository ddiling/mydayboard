package io.github.ddiling.mydayboard.buildlogic

import com.android.build.api.dsl.CommonExtension
import com.android.build.api.dsl.DefaultConfig

/**
 * Settings that must be identical in every Android module (apps and libraries):
 * SDK levels, instrumentation runner and Java compatibility. One place, so the
 * application and library conventions cannot drift apart.
 *
 * AGP 9 only offers the `defaultConfig { }` block on the concrete extensions,
 * so the shared defaults are a second function that both plugins call from it.
 */
internal fun CommonExtension.configureAndroidCommon() {
    compileSdk { version = release(ProjectConfig.COMPILE_SDK) }

    compileOptions.sourceCompatibility = ProjectConfig.JAVA_VERSION
    compileOptions.targetCompatibility = ProjectConfig.JAVA_VERSION
}

internal fun DefaultConfig.configureAndroidDefaults() {
    minSdk = ProjectConfig.MIN_SDK
    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
}
