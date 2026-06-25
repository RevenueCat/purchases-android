// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application).apply(false)
    alias(libs.plugins.kotlin.android).apply(false)
}

allprojects {
    repositories {
        google()
        mavenCentral()
        maven(url = "https://maven.pkg.github.com/RevenueCat/purchases-android") {
            content { includeGroup("com.revenuecat.purchases") }
            credentials {
                username = providers.gradleProperty("githubPackagesUsername").orNull
                    ?: System.getenv("GITHUB_PACKAGES_USERNAME")
                password = providers.gradleProperty("githubPackagesToken").orNull
                    ?: System.getenv("GITHUB_PACKAGES_TOKEN")
            }
        }
    }
}

tasks.register<Delete>("clean") {
    delete(rootProject.layout.buildDirectory)
}
