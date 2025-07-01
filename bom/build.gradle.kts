import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
    kotlin("jvm")
}

if (!project.properties["ANDROID_VARIANT_TO_PUBLISH"].toString().contains("customEntitlementComputation")) {
    apply(plugin = "com.vanniktech.maven.publish")
}

kotlin {
    compilerOptions {
        languageVersion = KotlinVersion.fromVersion(libs.versions.kotlinLanguage.get())
        apiVersion = KotlinVersion.fromVersion(libs.versions.kotlinApi.get())
    }
}

dependencies {
    constraints {
        api(project(":purchases"))
        api(project(":ui:revenuecatui"))
        api(project(":ui:debugview"))
        api(project(":feature:amazon"))
    }
}
