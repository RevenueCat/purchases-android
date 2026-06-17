package com.revenuecat.purchases.paywallfixtures.tasks

import com.revenuecat.purchases.paywallfixtures.internal.FixtureRecorder
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option

/**
 * Fetches the offerings of a RevenueCat app and records them — together with the images their paywalls
 * reference — as fixtures for offline paywall snapshot testing with the `purchases-ui-testing` artifact.
 */
public abstract class RecordPaywallFixturesTask : DefaultTask() {

    // @Internal instead of @Input: the API key is a secret and must not be stored in Gradle build scans
    // or appear in --info logs as a cache key component. This task is never up to date anyway.
    @get:Internal
    public abstract val apiKey: Property<String>

    @get:Input
    public abstract val appUserId: Property<String>

    @get:Input
    public abstract val offerings: SetProperty<String>

    @get:Input
    public abstract val baseUrl: Property<String>

    @get:OutputDirectory
    public abstract val outputDirectory: DirectoryProperty

    @get:Input
    @get:Option(
        option = "refresh",
        description = "Re-download all assets, even ones already present in the fixture directory.",
    )
    public abstract val refresh: Property<Boolean>

    init {
        group = "revenuecat"
        description = "Records RevenueCat paywall fixtures for offline paywall snapshot testing"
        refresh.convention(false)
        // The dashboard can change at any time; always re-record when requested.
        outputs.upToDateWhen { false }
    }

    @TaskAction
    public fun record() {
        val key = apiKey.orNull?.takeIf { it.isNotBlank() }
            ?: throw GradleException(
                "A RevenueCat API key is required to record paywall fixtures. Set the REVENUECAT_API_KEY " +
                    "environment variable, or configure paywallFixtures { apiKey.set(...) } in your " +
                    "build script.",
            )

        FixtureRecorder(
            apiKey = key,
            baseUrl = baseUrl.get(),
            appUserId = appUserId.get(),
            offeringsFilter = offerings.getOrElse(emptySet()),
            outputDirectory = outputDirectory.get().asFile,
            refresh = refresh.get(),
            log = { message -> logger.lifecycle(message) },
        ).record()
    }
}
