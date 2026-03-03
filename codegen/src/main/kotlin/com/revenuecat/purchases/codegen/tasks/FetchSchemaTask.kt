package com.revenuecat.purchases.codegen.tasks

import com.revenuecat.purchases.codegen.OfflineMode
import com.revenuecat.purchases.codegen.api.ProjectSchema
import com.revenuecat.purchases.codegen.api.RevenueCatApiClient
import com.revenuecat.purchases.codegen.cache.SchemaCache
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

public abstract class FetchSchemaTask : DefaultTask() {

    // @Internal instead of @Input: the API key is a secret and must not be stored
    // in Gradle build scans or appear in --info logs as a cache key component.
    // FetchSchemaTask already uses `outputs.upToDateWhen { false }` so the key
    // does not need to participate in up-to-date checking.
    @get:Internal
    public abstract val apiKey: Property<String>

    @get:Input
    public abstract val projectId: Property<String>

    @get:Input
    public abstract val cacheTtlMinutes: Property<Long>

    @get:Input
    public abstract val offlineMode: Property<OfflineMode>

    @get:OutputDirectory
    public abstract val cacheDir: DirectoryProperty

    init {
        group = "revenuecat"
        description = "Fetches entitlement and offering schema from RevenueCat API v2"
        // Always evaluate the task action so the TTL check inside runs
        outputs.upToDateWhen { false }
    }

    @TaskAction
    public fun fetch() {
        val key = apiKey.orNull?.takeIf { it.isNotBlank() }
            ?: throw GradleException(
                "revenuecat.apiKey must be configured in your build script.",
            )
        val projectIdValue = projectId.orNull?.takeIf { it.isNotBlank() }
            ?: throw GradleException(
                "revenuecat.projectId must be configured in your build script.",
            )

        val cache = SchemaCache(cacheDir.get().asFile)

        if (cache.isValid(cacheTtlMinutes.get())) {
            logger.lifecycle("RevenueCat schema cache is still valid, skipping fetch.")
            return
        }

        try {
            val client = RevenueCatApiClient(key)
            logger.lifecycle("Fetching entitlements from RevenueCat API...")
            val entitlements = client.fetchEntitlements(projectIdValue)
            logger.lifecycle("Fetching offerings from RevenueCat API...")
            val offerings = client.fetchOfferings(projectIdValue)

            val schema = ProjectSchema(
                entitlements = entitlements,
                offerings = offerings,
            )

            cache.write(schema)
            logger.lifecycle(
                "Fetched ${entitlements.size} entitlements and ${offerings.size} offerings from RevenueCat.",
            )
        } catch (e: Exception) {
            handleFetchError(e, cache)
        }
    }

    private fun handleFetchError(e: Exception, cache: SchemaCache) {
        when (offlineMode.get()) {
            OfflineMode.FAIL -> {
                throw GradleException("Failed to fetch RevenueCat schema: ${e.message}", e)
            }
            OfflineMode.USE_CACHE_OR_SKIP -> {
                val cached = cache.read()
                if (cached != null) {
                    val ageDesc = cache.cacheAgeMinutes()
                        ?.let { " (cached ${it}m ago)" }
                        ?: ""
                    logger.warn(
                        "Failed to fetch RevenueCat schema (${e.message}). " +
                            "Using stale cached data$ageDesc.",
                    )
                } else {
                    logger.warn(
                        "Failed to fetch RevenueCat schema (${e.message}). " +
                            "No cache available, skipping code generation.",
                    )
                }
            }
        }
    }
}
