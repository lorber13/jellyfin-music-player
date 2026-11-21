package io.github.lorber13.jellyfinmusic

import androidx.activity.ComponentActivity
import dagger.hilt.android.AndroidEntryPoint

/**
 * An empty activity for instrumented tests of screens that use Hilt ViewModels. It lives in the
 * debug build rather than in androidTest: in an application module, activities of the test APK
 * don't run in the app's process, so Hilt couldn't inject them.
 */
@AndroidEntryPoint
class HiltTestActivity : ComponentActivity()
