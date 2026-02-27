import org.jetbrains.kotlin.gradle.plugin.KotlinBasePluginWrapper
import java.lang.Runtime.Version

plugins {
    alias(libs.plugins.kotlin.jvm)
}

private val kotlinPlugin = plugins
    .withType<KotlinBasePluginWrapper>()
    .firstOrNull()
    ?: error("Kotlin plugin is not applied")

if (
    !project.properties["ANDROID_VARIANT_TO_PUBLISH"].toString().contains("customEntitlementComputation") &&
    // This will ensure the bom automatically gets published once we update to Kotlin 1.9.20.
    Version.parse(kotlinPlugin.pluginVersion) >= Version.parse("1.9.20")
) {
    apply(plugin = "com.vanniktech.maven.publish")
}

dependencies {
    constraints {
        api(project(":purchases"))
        api(project(":ui:revenuecatui"))
        api(project(":ui:debugview"))
        api(project(":feature:amazon"))
        val hasSamsungIapAar = (rootProject.extra["hasSamsungIapAar"] as? Boolean) == true
        if (hasSamsungIapAar) {
            api(project(":feature:galaxy"))
        }
        api(project(":feature:admob"))
    }
}
