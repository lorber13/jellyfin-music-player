import org.gradle.api.Plugin
import org.gradle.api.Project

/** Adds Hilt dependency injection (with KSP code generation) to an Android module. */
class HiltConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.pluginManager.apply("com.google.devtools.ksp")
        target.pluginManager.apply("com.google.dagger.hilt.android")

        target.dependencies.apply {
            add("implementation", target.catalogLibrary("hilt-android"))
            add("ksp", target.catalogLibrary("hilt-compiler"))
        }
    }
}
