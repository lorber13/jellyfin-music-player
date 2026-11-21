import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// The project's own Lint checks. The convention plugins add them to every Android module.
plugins {
    alias(libs.plugins.kotlin.jvm)
    // Lints the checks themselves, e.g. for wrong use of the Lint API.
    alias(libs.plugins.android.lint)
}

// Kotlin and Java must target the same version, whatever JDK runs Gradle.
java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions.jvmTarget = JvmTarget.JVM_17
}

lint {
    warningsAsErrors = true
    abortOnError = true
}

dependencies {
    // Lint provides its own API at runtime.
    compileOnly(libs.lint.api)

    testImplementation(libs.junit)
    testImplementation(libs.lint.cli)
    testImplementation(libs.lint.tests)
}
