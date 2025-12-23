pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven(url = "https://s01.oss.sonatype.org/content/repositories/snapshots/") {
            content {
                includeGroup("com.revenuecat.purchases")
            }
        }
    }
}

rootProject.name = "AdMobIntegrationSample"
include(":app")
