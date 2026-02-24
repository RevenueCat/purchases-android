import java.nio.file.Files

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

val samsungIapSdkDir = file("$rootDir/libs")

/**
 * Returns true only when the expected Samsung IAP AAR (versioned from
 * `gradle/libs.versions.toml`) is present in the SDK directory.
 */
@Suppress("ReturnCount")
private fun Settings.isSamsungIAPAARPresent(samsungIAPSDKDir: File): Boolean {
    if (!samsungIAPSDKDir.exists()) { return false }

    val samsungIapVersion = readVersionFromCatalog("samsungIap") ?: return false
    val samsungIapAar = samsungIAPSDKDir.resolve("samsung-iap-$samsungIapVersion.aar")
    return samsungIapAar.isFile
}

/**
 * Reads a version entry from `gradle/libs.versions.toml` without relying on the
 * version catalog extension (not available during settings evaluation).
 */
@Suppress("ReturnCount")
private fun Settings.readVersionFromCatalog(versionKey: String): String? {
    val catalogFile = rootDir.resolve("gradle/libs.versions.toml")
    if (!catalogFile.isFile) {
        return null
    }
    val lineRegex = Regex("^\\s*$versionKey\\s*=\\s*\"([^\"]+)\"\\s*$")
    Files.readAllLines(catalogFile.toPath()).forEach { line ->
        val match = lineRegex.find(line)
        if (match != null) {
            return match.groupValues[1]
        }
    }
    return null
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

        // Local Samsung IAP SDK AAR
        flatDir {
            dirs(samsungIapSdkDir)
        }
    }
}

include(":feature:amazon")
val samsungIAPAARPresent = isSamsungIAPAARPresent(samsungIapSdkDir)
gradle.beforeProject {
    if (this == rootProject) {
        extra["hasSamsungIapAar"] = samsungIAPAARPresent
    }
}
if (samsungIAPAARPresent) {
    include(":feature:galaxy")
}
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
