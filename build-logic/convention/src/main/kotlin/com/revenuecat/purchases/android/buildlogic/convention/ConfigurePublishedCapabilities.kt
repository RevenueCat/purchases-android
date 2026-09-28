package com.revenuecat.purchases.android.buildlogic.convention

import org.gradle.api.Project

private const val ADDITIONAL_CAPABILITIES_PROPERTY = "POM_ADDITIONAL_CAPABILITIES"

/**
 * Adds any capabilities declared through [ADDITIONAL_CAPABILITIES_PROPERTY] to the variants exposed by a public
 * library. Declaring an explicit capability replaces Gradle's implicit capability, so the publication's own
 * coordinates are added explicitly as well.
 */
internal fun Project.configurePublishedCapabilities() {
    val additionalCapabilityNames = findProperty(ADDITIONAL_CAPABILITIES_PROPERTY)
        ?.toString()
        ?.split(',')
        ?.map(String::trim)
        ?.filter(String::isNotEmpty)
        .orEmpty()

    if (additionalCapabilityNames.isEmpty()) return

    val publishedGroup = property("GROUP")
    val publishedArtifact = property("POM_ARTIFACT_ID")
    val publishedVersion = property("VERSION_NAME")

    configurations.configureEach {
        if (!isCanBeConsumed) return@configureEach

        // AGP uses Elements configurations for project dependencies and separate Publication configurations for
        // Gradle Module Metadata. Since explicit capabilities replace the implicit one, retain the identity that
        // consumers request in each context.
        val ownCapability = when {
            name.endsWith("ApiElements") || name.endsWith("RuntimeElements") ->
                "${project.group}:${project.name}:${project.version}"
            name.endsWith("ApiPublication") || name.endsWith("RuntimePublication") ->
                "$publishedGroup:$publishedArtifact:$publishedVersion"
            else -> return@configureEach
        }

        outgoing.capability(ownCapability)
        additionalCapabilityNames.forEach { capabilityName ->
            outgoing.capability("$publishedGroup:$capabilityName:$publishedVersion")
        }
    }
}
