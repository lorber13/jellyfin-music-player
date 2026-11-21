import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.Plugin
import org.gradle.api.Project

class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.pluginManager.apply("com.android.application")

        val android = target.extensions.getByType(ApplicationExtension::class.java)
        configureAndroidCommon(android)
        target.addProjectLintChecks()
        android.defaultConfig.targetSdk = AndroidSdkLevels.TARGET
    }
}
