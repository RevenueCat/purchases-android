package com.revenuecat.purchases.common.remoteconfig

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.common.Backend
import com.revenuecat.purchases.common.DateProvider
import com.revenuecat.purchases.common.DefaultDateProvider
import com.revenuecat.purchases.common.debugLog
import com.revenuecat.purchases.common.errorLog
import com.revenuecat.purchases.common.networking.RCContainer
import kotlinx.serialization.SerializationException
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Orchestrates a single `/v1/config` sync: replays the persisted opaque manifest, then on `204` keeps the cache
 * untouched and on `200` persists the fresh server manifest plus the per-topic blob refs and only then, gated on
 * a successful persist, writes the inlined blobs the resolved config still wants and prunes the rest. It reports
 * which prefetch blobs are now cached locally on the next request.
 *
 * The manifest is opaque (stored and replayed verbatim); the active-topic set and removed-topic detection come
 * from the response's [RemoteConfiguration.activeTopics]. Only blobs that arrive inlined in the response are
 * handled here; fetching missing prefetch blobs over the network (Phase 5, once blob sources are resolved),
 * topic-handler dispatch (Phase 4) and lifecycle wiring (Phase 7) are not done here yet.
 *
 * Overlapping refreshes are deduped: only one [refreshRemoteConfig] runs at a time. A call made while one is
 * already in flight is skipped (the backend collapses concurrent requests but still fires every callback, which
 * would otherwise parse and persist the same response more than once).
 */
@OptIn(InternalRevenueCatAPI::class)
internal class RemoteConfigManager(
    private val backend: Backend,
    private val diskCache: RemoteConfigDiskCache,
    private val blobStore: RemoteConfigBlobStore,
    private val dateProvider: DateProvider = DefaultDateProvider(),
) {
    private val isRefreshing = AtomicBoolean(false)

    fun refreshRemoteConfig(appInBackground: Boolean, appUserID: String) {
        if (isRefreshing.getAndSet(true)) {
            debugLog { "Remote config refresh already in progress. Skipping." }
            return
        }
        val persisted = diskCache.read()
        backend.getRemoteConfig(
            appInBackground = appInBackground,
            appUserID = appUserID,
            domain = persisted?.domain ?: DEFAULT_DOMAIN,
            // Opaque manifest replayed verbatim; null on the first run when nothing is persisted yet.
            manifest = persisted?.manifest,
            // Report only the prefetch blobs we actually hold, so the server stops re-inlining them.
            prefetchedBlobs = persisted?.prefetchBlobs?.filter { blobStore.contains(it) } ?: emptyList(),
            onSuccess = { container, _ ->
                // Hold the in-flight guard until persistence completes so a concurrent refresh can't read the
                // disk cache mid-write and merge against a stale snapshot.
                try {
                    if (container != null) {
                        val response = RemoteConfiguration.parse(container.config.data)
                        persist(previous = persisted, response = response, container = container)
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
        response: RemoteConfiguration,
        container: RCContainer,
    ) {
        val previousBlobRefs = previous?.topicBlobRefs ?: emptyMap()
        // Changed topics overwrite their refs; unchanged topics keep their carried-forward refs (the server
        // omits them); topics no longer active are pruned.
        val mergedBlobRefs = (previousBlobRefs + response.topics.toTopicBlobRefs())
            .filterKeys { it in response.activeTopics }

        // Blobs the current config still wants: the prefetch set plus any active-topic blob ref.
        val blobRefsToKeep = response.prefetchBlobs.toSet() + mergedBlobRefs.values.flatten()

        // Persist the configuration first: it is the source of truth (the manifest the server diffs against),
        // whereas inline blobs are recoverable over the network. Only touch the blob store
        // once the state is durably committed, so a failed persist never orphans or evicts blobs.
        val persisted = diskCache.write(
            PersistedRemoteConfigurationState(
                domain = response.domain,
                manifest = response.manifest,
                activeTopics = response.activeTopics,
                prefetchBlobs = response.prefetchBlobs,
                topicBlobRefs = mergedBlobRefs,
                lastRefreshAt = dateProvider.now.time,
            ),
        )

        if (persisted) {
            extractInlineBlobs(container, blobRefsToKeep)
            blobStore.retainOnly(blobRefsToKeep)
        } else {
            errorLog { "Skipping remote config blob sync: failed to persist the configuration." }
        }
    }

    /** Caches inlined content elements the config still wants, whose bytes match their content-address ref. */
    private fun extractInlineBlobs(container: RCContainer, refsToKeep: Set<String>) {
        container.elements.forEach { (ref, element) ->
            if (ref !in refsToKeep) return@forEach
            if (element.isChecksumValid()) {
                blobStore.write(ref, element.data)
            } else {
                errorLog { "Skipping remote config blob '$ref': checksum verification failed." }
            }
        }
    }

    private companion object {
        private const val DEFAULT_DOMAIN = "app"
    }
}

/** The blob refs each topic's items reference, keyed by topic name (empty list for inline-only topics). */
internal fun Map<String, ConfigTopic>.toTopicBlobRefs(): Map<String, List<String>> =
    mapValues { (_, topic) -> topic.values.mapNotNull { it.blobRef } }
