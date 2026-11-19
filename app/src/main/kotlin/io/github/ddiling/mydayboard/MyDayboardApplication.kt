package io.github.ddiling.mydayboard

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Exists only for `@HiltAndroidApp`: Hilt generates the app-wide (singleton) component here,
 * the root that every activity and ViewModel component is created from.
 * Registered as `android:name` in the manifest.
 */
@HiltAndroidApp
class MyDayboardApplication : Application()
