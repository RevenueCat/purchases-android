package com.revenuecat.purchases.common.remoteconfig

import com.revenuecat.purchases.common.Backend
import com.revenuecat.purchases.common.errorLog
import kotlinx.serialization.SerializationException

/**
 * Orchestrates a single `/v2/config` sync: replays the persisted [RemoteConfiguration.Manifest], then on `204`
 * keeps the cache untouched and on `200` persists the fresh server manifest plus the resolved topic bodies.
 *
 * Blob extraction (Phase 3), topic-handler dispatch (Phase 4) and lifecycle wiring (Phase 7) are not done here
 * yet; this manager currently only owns manifest replay and persistence.
 */
internal class RemoteConfigManager(
    private val backend: Backend,
    private val diskCache: RemoteConfigDiskCache,
) {
    fun refreshRemoteConfig(appInBackground: Boolean) {
        val persisted = diskCache.read()
        val requestManifest = persisted?.manifest ?: RemoteConfiguration.Manifest(domain = DEFAULT_DOMAIN)
        backend.getRemoteConfig(
            appInBackground = appInBackground,
            manifest = requestManifest,
            onSuccess = { container, _ ->
                if (container == null) {
                    // 204: nothing changed, keep the cache.
                    return@getRemoteConfig
                }
                try {
                    val response = RemoteConfiguration.parse(container.config.data)
                    persist(previous = persisted, response = response)
                } catch (e: SerializationException) {
                    errorLog(e) { "Failed to parse remote config response. Keeping the cached configuration." }
                }
            },
            onError = { error ->
                errorLog(error)
            },
        )
    }

    private fun persist(previous: PersistedRemoteConfig?, response: RemoteConfiguration) {
        val previousTopics = previous?.topics ?: emptyMap()
        // Changed bodies overwrite previous ones; topics dropped from the new manifest are pruned.
        val mergedTopics = (previousTopics + response.topics)
            .filterKeys { it in response.manifest.topics.keys }
        diskCache.write(response.manifest, mergedTopics)
    }

    private companion object {
        private const val DEFAULT_DOMAIN = "app"
    }
}
