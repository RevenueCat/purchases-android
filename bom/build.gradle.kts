plugins {
    alias(libs.plugins.kotlin.jvm)
}

if (!project.properties["ANDROID_VARIANT_TO_PUBLISH"].toString().contains("customEntitlementComputation")) {
    apply(plugin = "com.vanniktech.maven.publish")
}

dependencies {
    constraints {
        api(project(":purchases"))
        api(project(":ui:revenuecatui"))
        api(project(":ui:debugview"))
        api(project(":feature:amazon"))
        api(project(":feature:galaxy"))
        api(project(":feature:admob"))
        api(project(":feature:admob-next-gen"))
        api(project(":codegen"))
    }
}
