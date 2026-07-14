package com.revenuecat.purchases.common.uiconfig

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.UiConfig
import com.revenuecat.purchases.common.remoteconfig.GenerationGuardedCache
import com.revenuecat.purchases.common.remoteconfig.RemoteConfigCommitListener
import com.revenuecat.purchases.common.remoteconfig.RemoteConfigManager
import com.revenuecat.purchases.common.remoteconfig.RemoteConfigTopic
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * The topic-specific front door for `ui_config`: four independently-updated parts — `app`, `localizations`,
 * `variable_config`, `custom_variables` — that together make up one [UiConfig], the same shape the legacy
 * offerings response sends pre-assembled in a single JSON object. Each part is its own blob-ref item under the
 * topic (not inline metadata). The parts are resolved concurrently and merged into a single keyed object via
 * [RemoteConfigManager.mergeItemsBlobData], whose item-key-to-blob shape matches [UiConfig]'s wire format
 * exactly, so the merged object decodes straight into [UiConfig] — including the property-level localizations
 * serializer that skips unknown variable localization keys.
 *
 * The merge is all-or-nothing: if any part is missing, unresolvable, or the merged object doesn't decode, no
 * [UiConfig] is returned. Callers that need UI config to render should fail instead of using a partial/default
 * configuration.
 *
 * `ui_config` is always kept in memory once resolved, so [getUiConfig] is memory-first: a warm read returns
 * synchronously (the suspend fn never suspends, so it resumes on the caller's thread with no dispatch) and only
 * a miss touches the config layer. The in-memory copy is re-warmed on every config commit and dropped on
 * identity change / disable (via [RemoteConfigCommitListener]); a [RemoteConfigManager.configGeneration] guard
 * makes sure a slower disk warm never clobbers a fresher network commit (store-if-newer).
 */
@OptIn(InternalRevenueCatAPI::class)
internal class UiConfigProvider(
    private val manager: RemoteConfigManager,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
) : RemoteConfigCommitListener {

    private val cache = GenerationGuardedCache<UiConfig>()

    /** Whether the `ui_config` is already in memory, so a caller can deliver synchronously. */
    fun isWarm(): Boolean = cache.isWarm()

    /**
     * Returns the in-memory [UiConfig] if present, else resolves it from the config layer (which may wait for or
     * trigger a `/v1/config` sync on a cold cache), caches it, and returns it. `null` when it can't be resolved.
     *
     * On a miss the resolved value is only returned via the cache (store-if-newer): if an identity-change
     * invalidation advanced the generation while [resolve] was in flight, [GenerationGuardedCache.store] drops
     * it and [GenerationGuardedCache.cached] is `null`, so the previous user's config is never served.
     */
    suspend fun getUiConfig(): UiConfig? {
        cache.cached?.let { return it }
        val generation = manager.configGeneration
        resolve()?.let { cache.store(generation, it) }
        return cache.cached
    }

    /**
     * Best-effort populate of the in-memory cache from already-committed config, tagged with [generation].
     * No-op (no network) when `ui_config` isn't committed yet, so a cold-disk init warm never triggers a sync.
     */
    suspend fun warm(generation: Int) {
        if (manager.committedTopicOrNull(RemoteConfigTopic.UiConfig) == null) return
        val uiConfig = resolve() ?: return
        cache.store(generation, uiConfig)
    }

    /** Warms at the current config generation; used by the offerings readiness gate. */
    suspend fun warm() = warm(manager.configGeneration)

    /** Fire-and-forget [warm] on this provider's own scope; used for the cold-start init warm. */
    fun warmAsync(generation: Int) {
        scope.launch { warm(generation) }
    }

    override fun onConfigCommitted(generation: Int) {
        scope.launch { warm(generation) }
    }

    override fun onConfigInvalidated(generation: Int) {
        cache.invalidate(generation)
    }

    fun close() {
        scope.cancel()
    }

    private suspend fun resolve(): UiConfig? =
        manager.mergeItemsBlobData<UiConfig>(RemoteConfigTopic.UiConfig, ITEM_KEYS)

    private companion object {
        private val ITEM_KEYS = listOf("app", "localizations", "variable_config", "custom_variables")
    }
}
