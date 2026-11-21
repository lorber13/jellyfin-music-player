import com.android.build.api.dsl.CommonExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.artifacts.MinimalExternalModuleDependency
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.provider.Provider

internal object AndroidSdkLevels {
    // Compose 1.12 requires compiling against API 37.
    const val COMPILE = 37
    const val TARGET = 37

    // Android 10, the oldest version the product supports.
    const val MINIMUM = 29
}

/** Settings shared by the application module and every library module. */
internal fun configureAndroidCommon(extension: CommonExtension) {
    extension.compileSdk = AndroidSdkLevels.COMPILE
    extension.defaultConfig.minSdk = AndroidSdkLevels.MINIMUM

    // Kotlin's jvmTarget follows targetCompatibility with AGP's built-in Kotlin support.
    extension.compileOptions.sourceCompatibility = JavaVersion.VERSION_17
    extension.compileOptions.targetCompatibility = JavaVersion.VERSION_17

    extension.lint.warningsAsErrors = true
    extension.lint.abortOnError = true
    // These checks fail the build whenever a new library or plugin version is published,
    // which would break unrelated work. Dependency updates are done deliberately instead.
    extension.lint.disable += setOf("GradleDependency", "NewerVersionAvailable", "AndroidGradlePluginVersion")
}

/** Runs the project's own Lint checks from :lint-rules, such as HardcodedComposeText, on this module. */
internal fun Project.addProjectLintChecks() {
    dependencies.add("lintChecks", dependencies.project(":lint-rules"))
}

/** Looks up a library from gradle/libs.versions.toml by its alias, e.g. "hilt-android". */
internal fun Project.catalogLibrary(alias: String): Provider<MinimalExternalModuleDependency> =
    extensions.getByType(VersionCatalogsExtension::class.java).named("libs").findLibrary(alias).get()
