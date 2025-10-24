pluginManagement {
    repositories {
        google()
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
rootProject.name = "CustomEntitlementComputationSample"
include(":app")

// Uncomment to use local version of the SDK
// includeBuild("../../") {
//    dependencySubstitution {
//        substitute(
//            module("com.revenuecat.purchases:purchases-custom-entitlement-computation")
//        ).using(project(":purchases"))
//    }
// }
