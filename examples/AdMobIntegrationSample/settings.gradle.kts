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

// Using local version of the SDK since 9.17.0-SNAPSHOT isn't published yet
includeBuild("../..") {
   dependencySubstitution {
       substitute(module("com.revenuecat.purchases:purchases"))
           .using(project(":purchases"))
   }
}
