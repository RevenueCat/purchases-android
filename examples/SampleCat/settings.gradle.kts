pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven(url = "https://central.sonatype.com/repository/maven-snapshots/") {
            content { includeGroup("com.revenuecat.purchases") }
        }
    }
}

rootProject.name = "SampleCat"
include(":app")

// CI passes -PuseLocalSdk to build against the SDK sources in this repo, so a change that breaks the app fails
// its PR checks. A downloaded copy of the app does not set it and uses the published SDK.
if (providers.gradleProperty("useLocalSdk").isPresent) {
    includeBuild("../../") {
        dependencySubstitution {
            substitute(module("com.revenuecat.purchases:purchases")).using(project(":purchases"))
            substitute(module("com.revenuecat.purchases:purchases-ui")).using(project(":ui:revenuecatui"))
        }
    }
}
