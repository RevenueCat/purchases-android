package com.revenuecat.purchases.common.workflows

import com.revenuecat.purchases.common.remoteconfig.ConfigTopic
import com.revenuecat.purchases.common.remoteconfig.ConfigTopicHandler
import com.revenuecat.purchases.common.remoteconfig.RemoteConfigBlobFetcher
import com.revenuecat.purchases.common.remoteconfig.RemoteConfigBlobStore
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

/**
 * Applies the `workflows` topic. The item **metadata** (`offering_identifier`, `blob_ref`, `prefetch`) is
 * persisted generically by [com.revenuecat.purchases.common.remoteconfig.RemoteConfigManager] as part of the
 * sync, so this handler no longer owns any storage of its own. Its only job is to **prefetch** the bodies the
 * server marked `prefetch: true`, through the [RemoteConfigBlobFetcher] seam.
 *
 * Prefetch is best-effort: a failed (or, until the fetch phase lands, no-op) download is swallowed — the body is
 * fetched on demand at read time instead — so it never throws and never rolls back the sync.
 */
internal class WorkflowsTopicHandler(
    private val blobFetcher: RemoteConfigBlobFetcher,
) : ConfigTopicHandler {

    override val topicName: String = TOPIC_NAME

    override suspend fun handle(items: ConfigTopic, blobStore: RemoteConfigBlobStore) {
        val refsToPrefetch = items.values.filter { it.prefetch }.mapNotNull { it.blobRef }
        if (refsToPrefetch.isEmpty()) return
        coroutineScope {
            refsToPrefetch.map { ref -> async { blobFetcher.ensureDownloaded(ref) } }.awaitAll()
        }
    }

    override fun clear() {
        // No handler-owned state: the manager prunes this topic's persisted metadata when it leaves the
        // active set, and blob retention drops any body it kept alive.
    }

    private companion object {
        private const val TOPIC_NAME = "workflows"
    }
}
