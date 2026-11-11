import com.android.build.api.dsl.CommonExtension
import org.gradle.api.Plugin
import org.gradle.api.Project

/** Turns on Jetpack Compose in a module that already applies an Android convention plugin. */
class AndroidComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.pluginManager.apply("org.jetbrains.kotlin.plugin.compose")

        val android = target.extensions.getByType(CommonExtension::class.java)
        android.buildFeatures.compose = true

        val composeBom = target.dependencies.platform(target.catalogLibrary("compose-bom"))
        target.dependencies.apply {
            add("implementation", composeBom)
            add("androidTestImplementation", composeBom)
            add("implementation", target.catalogLibrary("compose-ui"))
            add("implementation", target.catalogLibrary("compose-material3"))
            // Declared explicitly because Compose alone pulls in an older lifecycle version,
            // whose bundled lint checks no longer run on the current Android Gradle Plugin.
            add("implementation", target.catalogLibrary("androidx-lifecycle-runtime-compose"))
            add("implementation", target.catalogLibrary("compose-ui-tooling-preview"))
            add("debugImplementation", target.catalogLibrary("compose-ui-tooling"))
        }
    }
}
