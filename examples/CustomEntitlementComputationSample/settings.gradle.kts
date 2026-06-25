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
