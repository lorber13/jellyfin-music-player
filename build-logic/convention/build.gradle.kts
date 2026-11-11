plugins {
    `kotlin-dsl`
}

dependencies {
    // compileOnly: at runtime the Android Gradle Plugin comes from the root project's plugins block.
    compileOnly(libs.android.gradle.plugin)
}

gradlePlugin {
    plugins {
        register("androidApplication") {
            id = "jellyfinmusic.android.application"
            implementationClass = "AndroidApplicationConventionPlugin"
        }
        register("androidLibrary") {
            id = "jellyfinmusic.android.library"
            implementationClass = "AndroidLibraryConventionPlugin"
        }
        register("androidCompose") {
            id = "jellyfinmusic.android.compose"
            implementationClass = "AndroidComposeConventionPlugin"
        }
        register("hilt") {
            id = "jellyfinmusic.hilt"
            implementationClass = "HiltConventionPlugin"
        }
    }
}
