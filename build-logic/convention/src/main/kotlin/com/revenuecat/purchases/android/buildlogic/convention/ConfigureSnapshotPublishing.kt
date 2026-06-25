package com.revenuecat.purchases.android.buildlogic.convention

import com.revenuecat.purchases.android.buildlogic.ktx.libs
import com.revenuecat.purchases.android.buildlogic.ktx.plugins
import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.tasks.PublishToMavenRepository
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.maven

private const val GITHUB_PACKAGES_REPOSITORY_NAME = "GitHubPackages"
private const val GITHUB_PACKAGES_URL = "https://maven.pkg.github.com/RevenueCat/purchases-android"

/**
 * Points SNAPSHOT publishing at GitHub Packages and blocks Maven Central; releases are unaffected
 * and keep publishing to Maven Central.
 *
 * For SNAPSHOT versions this registers the GitHub Packages repository and disables every Maven
 * Central publish task, so a snapshot cannot reach Central even via `publish` or
 * `publishToMavenCentral`.
 *
 * Credentials come from the `githubPackagesUsername` / `githubPackagesToken` Gradle properties,
 * falling back to the `GITHUB_PACKAGES_USERNAME` / `GITHUB_PACKAGES_TOKEN` environment variables.
 */
internal fun Project.configureSnapshotPublishing() {
    val versionName = providers.gradleProperty("VERSION_NAME").orNull ?: version.toString()
    if (!versionName.endsWith("-SNAPSHOT")) return

    val githubPackagesUsername = providers.gradleProperty("githubPackagesUsername").orNull
        ?: System.getenv("GITHUB_PACKAGES_USERNAME")
    val githubPackagesToken = providers.gradleProperty("githubPackagesToken").orNull
        ?: System.getenv("GITHUB_PACKAGES_TOKEN")

    pluginManager.withPlugin(libs.plugins.mavenPublish.get().pluginId) {
        extensions.configure<PublishingExtension> {
            repositories.maven(GITHUB_PACKAGES_URL) {
                name = GITHUB_PACKAGES_REPOSITORY_NAME
                credentials {
                    username = githubPackagesUsername
                    password = githubPackagesToken
                }
            }
        }

        tasks.withType(PublishToMavenRepository::class.java)
            .matching { !it.name.contains(GITHUB_PACKAGES_REPOSITORY_NAME) }
            .configureEach { enabled = false }
        tasks.matching { it.name == "publishToMavenCentral" || it.name == "publishAndReleaseToMavenCentral" }
            .configureEach { enabled = false }
    }
}
