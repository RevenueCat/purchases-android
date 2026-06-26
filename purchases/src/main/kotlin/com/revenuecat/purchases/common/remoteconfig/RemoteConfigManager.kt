package com.revenuecat.purchases.common.remoteconfig

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.common.Backend
import com.revenuecat.purchases.common.DateProvider
import com.revenuecat.purchases.common.DefaultDateProvider
import com.revenuecat.purchases.common.debugLog
import com.revenuecat.purchases.common.errorLog
import kotlinx.serialization.SerializationException
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Orchestrates a single `/v1/config` sync: replays the persisted opaque manifest, then on `204` keeps the cache
 * untouched and on `200` persists the fresh server manifest plus the resolved per-topic blob refs.
 *
 * The manifest is opaque (stored and replayed verbatim); the active-topic set and removed-topic detection come
 * from the response's [RemoteConfiguration.activeTopics]. Blob extraction (Phase 3), topic-handler dispatch
 * (Phase 4) and lifecycle wiring (Phase 7) are not done here yet; this manager currently only owns manifest
 * replay and persistence.
 *
 * Overlapping refreshes are deduped: only one [refreshRemoteConfig] runs at a time. A call made while one is
 * already in flight is skipped (the backend collapses concurrent requests but still fires every callback, which
 * would otherwise parse and persist the same response more than once).
 */
@OptIn(InternalRevenueCatAPI::class)
internal class RemoteConfigManager(
    private val backend: Backend,
    private val diskCache: RemoteConfigDiskCache,
    private val dateProvider: DateProvider = DefaultDateProvider(),
) {
    private val isRefreshing = AtomicBoolean(false)

    fun refreshRemoteConfig(appInBackground: Boolean, appUserID: String) {
        if (isRefreshing.getAndSet(true)) {
            debugLog { "Remote config refresh already in progress. Skipping." }
            return
        }
        val persisted = diskCache.read()?.takeIf { it.appUserID == appUserID }
        backend.getRemoteConfig(
            appInBackground = appInBackground,
            appUserID = appUserID,
            domain = persisted?.domain ?: DEFAULT_DOMAIN,
            // Opaque manifest replayed verbatim; null on the first run when nothing is persisted yet.
            manifest = persisted?.manifest,
            // No blob store yet (Phase 3 reports the refs actually cached); the SDK holds nothing here.
            prefetchedBlobs = emptyList(),
            onSuccess = { container, _ ->
                // Hold the in-flight guard until persistence completes so a concurrent refresh can't read the
                // disk cache mid-write and merge against a stale snapshot.
                try {
                    if (container != null) {
                        val response = RemoteConfiguration.parse(container.config.data)
                        persist(previous = persisted, appUserID = appUserID, response = response)
                    }
                    // container == null is a 204: nothing changed, keep the cache.
                } catch (e: SerializationException) {
                    errorLog(e) { "Failed to parse remote config response. Keeping the cached configuration." }
                } finally {
                    isRefreshing.set(false)
                }
            },
            onError = { error ->
                isRefreshing.set(false)
                errorLog(error)
            },
        )
    }

    private fun persist(
        previous: PersistedRemoteConfigurationState?,
        appUserID: String,
        response: RemoteConfiguration,
    ) {
        val previousBlobRefs = previous?.topicBlobRefs ?: emptyMap()
        // Changed topics overwrite their refs; topics no longer active are pruned.
        val mergedBlobRefs = (previousBlobRefs + response.topics.toTopicBlobRefs())
            .filterKeys { it in response.activeTopics }
        diskCache.write(
            PersistedRemoteConfigurationState(
                appUserID = appUserID,
                domain = response.domain,
                manifest = response.manifest,
                activeTopics = response.activeTopics,
                prefetchBlobs = response.prefetchBlobs,
                topicBlobRefs = mergedBlobRefs,
                lastRefreshAt = dateProvider.now.time,
            ),
        )
    }

    private companion object {
        private const val DEFAULT_DOMAIN = "app"
    }
}

/** The blob refs each topic's items reference, keyed by topic name (empty list for inline-only topics). */
internal fun Map<String, ConfigTopic>.toTopicBlobRefs(): Map<String, List<String>> =
    mapValues { (_, topic) -> topic.values.mapNotNull { it.blobRef } }
