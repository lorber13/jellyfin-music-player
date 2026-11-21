package io.github.lorber13.jellyfinmusic.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/** The app shell with the tabs, shown once the user is signed in. Each tab has its own back stack inside it. */
@Serializable
data object AppShellRoute : NavKey
