pluginManagement {
    repositories {
        google()
        mavenCentral()
        maven(url = "https://oss.sonatype.org/content/repositories/snapshots")
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven(url = "https://oss.sonatype.org/content/repositories/snapshots")
    }
}
rootProject.name = "MagicWeatherCompose"
include(":app")

// Uncomment to use local version of the SDK
// includeBuild("../../") {
//    dependencySubstitution {
//        substitute(module("com.revenuecat.purchases:purchases"))
//            .using(project(":purchases"))
//    }
//    dependencySubstitution {
//        substitute(module("com.revenuecat.purchases:purchases-debug-view"))
//            .using(project(":ui:debugview"))
//    }
//    dependencySubstitution {
//        substitute(module("com.revenuecat.purchases:purchases-debug-view-noop"))
//            .using(project(":ui:debugview"))
//    }
// }
