pluginManagement {
    includeBuild("build-logic")
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "jellyfin-music-player"

include(":app")
include(":core:designsystem")
include(":core:data")
include(":core:playback")
include(":feature:albums")
include(":feature:login")
include(":feature:player")
include(":lint-rules")
