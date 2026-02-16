pluginManagement {
    includeBuild("build-logic")
    repositories {
        // fetch plugins from google maven (https://maven.google.com)
        google {
            content {
                includeGroupByRegex("androidx\\..*")
                includeGroupByRegex("com\\.android(\\..*|)")
                includeGroupByRegex("com\\.google\\.android\\..*")
                includeGroupByRegex("com\\.google\\.crypto\\..*")
                includeGroupByRegex("com\\.google\\.firebase(\\..*|)")
                includeGroupByRegex("com\\.google\\.gms(\\..*|)")
                includeGroupByRegex("com\\.google\\.prefab")
                includeGroupByRegex("com\\.google\\.testing\\.platform")
            }
            mavenContent {
                releasesOnly()
            }
        }

        // fetch plugins from gradle plugin portal (https://plugins.gradle.org)
        gradlePluginPortal()

        // fallback for the rest of the dependencies
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)

    repositories {
        // fetch plugins from google maven (https://maven.google.com)
        google {
            content {
                includeGroupByRegex("androidx\\..*")
                includeGroupByRegex("com\\.android(\\..*|)")
                includeGroupByRegex("com\\.google\\.android\\..*")
                includeGroupByRegex("com\\.google\\.crypto\\..*")
                includeGroupByRegex("com\\.google\\.firebase(\\..*|)")
                includeGroupByRegex("com\\.google\\.gms(\\..*|)")
                includeGroupByRegex("com\\.google\\.prefab")
                includeGroupByRegex("com\\.google\\.testing\\.platform")
            }
            mavenContent {
                releasesOnly()
            }
        }

        // fallback for the rest of the dependencies
        mavenCentral()
    }
}

include(":feature:amazon")
include(":feature:admob")
include(":integration-tests")
include(":purchases")
include(":examples:purchase-tester")
include(":api-tester")
include(":ui:debugview")
include(":ui:revenuecatui")
include(":bom")
include(":examples:paywall-tester")
include(":test-apps:testpurchasesandroidcompatibility")
include(":test-apps:testpurchasesuiandroidcompatibility")
include(":examples:web-purchase-redemption-sample")
include(":examples:admob-sample")
include(":dokka-hide-internal")
include(":baselineprofile")
include(":test-apps:e2etests")
include(":examples:rcttester")
