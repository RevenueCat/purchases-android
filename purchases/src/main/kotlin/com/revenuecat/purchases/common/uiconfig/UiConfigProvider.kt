package com.revenuecat.purchases.common.uiconfig

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.UiConfig
import com.revenuecat.purchases.common.errorLog
import com.revenuecat.purchases.common.remoteconfig.GenerationGuardedCache
import com.revenuecat.purchases.common.remoteconfig.RemoteConfigCommitListener
import com.revenuecat.purchases.common.remoteconfig.RemoteConfigManager
import com.revenuecat.purchases.common.remoteconfig.RemoteConfigTopic
import com.revenuecat.purchases.common.remoteconfig.readConsistent
import com.revenuecat.purchases.common.verboseLog
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
 * configuration. [resolveUiConfig] classifies *why* nothing was returned — an absent/empty topic (a project
 * with no paywalls) is a valid outcome, not a failure — while [getUiConfig] is the plain
 * [UiConfigResolution.Found]-only view of it.
 *
 * `ui_config` is always kept in memory once resolved, so [getUiConfig] is memory-first: a warm read returns
 * synchronously (the suspend fn never suspends, so it resumes on the caller's thread with no dispatch) and only
 * a miss touches the config layer. The in-memory copy is re-warmed on every config commit and dropped on
 * identity change / disable (via [RemoteConfigCommitListener]); a [RemoteConfigManager.configGeneration] guard
 * makes sure a slower disk warm never clobbers a fresher network commit (store-if-newer). Cold reads are
 * guarded by [readConsistent], so a read superseded mid-resolve re-resolves against the new state instead of
 * serving it.
 */
@OptIn(InternalRevenueCatAPI::class)
@Suppress("TooManyFunctions")
internal class UiConfigProvider(
    private val manager: RemoteConfigManager,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
) : RemoteConfigCommitListener {

    private val cache = GenerationGuardedCache<UiConfig>()

    /** Whether the `ui_config` is already in memory, so a caller can deliver synchronously. */
    fun isWarm(): Boolean = cache.isWarm()

    /**
     * Returns the in-memory [UiConfig] if present, else resolves it from the config layer (which may wait for or
     * trigger a `/v1/config` sync on a cold cache), caches it, and returns it. `null` when it can't be resolved —
     * use [resolveUiConfig] when the reason matters.
     */
    suspend fun getUiConfig(): UiConfig? = (resolveUiConfig() as? UiConfigResolution.Found)?.uiConfig

    /**
     * Like [getUiConfig], but reports *why* no [UiConfig] came back, so a caller can tell a project that simply
     * has no `ui_config` (no paywalls configured) from one whose published `ui_config` couldn't be resolved. See
     * [UiConfigResolution] for what each outcome means.
     */
    suspend fun resolveUiConfig(): UiConfigResolution {
        cache.cached?.let { return UiConfigResolution.Found(it) }
        val resolution = manager.readConsistent(what = { "ui_config" }) { generation ->
            when (val resolved = resolve()) {
                null -> classifyUnresolved()
                else -> {
                    // Publish only while the read still looks consistent: a superseded value may belong to the
                    // previous user and must not land in the memory-first cache (the retry re-resolves, and the
                    // commit that superseded it re-warms the cache on its own).
                    if (manager.configGeneration == generation) cache.store(generation, resolved)
                    UiConfigResolution.Found(resolved)
                }
            }
        }
        // Superseded on both attempts: nothing trustworthy to serve or classify (an identity change has
        // already wiped the committed state the read saw). The next read re-resolves.
        return resolution ?: UiConfigResolution.Superseded
    }

    /**
     * Best-effort populate of the in-memory cache from already-committed config, tagged with [generation].
     * No-op (no network) when `ui_config` isn't committed yet, so a cold-disk init warm never triggers a sync.
     *
     * If the topic **is** committed but its body can't be resolved right now (e.g. a transient blob read),
     * the previous snapshot is dropped (store-if-newer [GenerationGuardedCache.invalidate]) rather than left
     * behind tagged with an older generation. Otherwise, since [getUiConfig] is memory-first and never falls
     * through on a hit, it would keep serving the outdated config after the generation advanced; dropping it
     * makes the next [getUiConfig] re-resolve the committed config instead.
     */
    suspend fun warm(generation: Int) {
        // Already warm for this (or a newer) generation — nothing to do.
        if (cache.isWarmAtOrAbove(generation)) return
        if (manager.committedTopicOrNull(RemoteConfigTopic.UiConfig) == null) return
        when (val uiConfig = resolve()) {
            null -> cache.invalidate(generation)
            else -> cache.store(generation, uiConfig)
        }
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

    private suspend fun resolve(): UiConfig? {
        if (manager.isDisabled) return null
        // topic() waits for or primes the initial config sync on a cold cache. Once that authoritative topic is
        // available, an absent topic or one with none of the ui_config parts means the project has no ui_config;
        // avoid asking the generic all-or-nothing merger to resolve four known-missing blobs and warning about it.
        var topic = manager.topic(RemoteConfigTopic.UiConfig)
        if (topic != null && ITEM_KEYS.none(topic::containsKey)) {
            topic = manager.committedTopicAfterInFlightRefresh(RemoteConfigTopic.UiConfig)
        }
        return if (topic == null || ITEM_KEYS.none(topic::containsKey)) {
            null
        } else {
            manager.mergeItemsBlobData<UiConfig>(RemoteConfigTopic.UiConfig, ITEM_KEYS)
        }
    }

    /**
     * Tells "there is nothing to resolve" apart from "resolving what is published failed". Only ever called
     * **after** a [resolve] attempt: a resolve on a cold cache waits for (or triggers) a `/v1/config` sync, so
     * the committed state read here is post-sync. Checking before resolving would see the cold cache and report
     * [UiConfigResolution.NotConfigured] for a project that does have a `ui_config`.
     */
    private suspend fun classifyUnresolved(): UiConfigResolution {
        // First: committedTopicOrNull also returns null when the endpoint is disabled, which would otherwise be
        // indistinguishable from an absent topic. ui_config is never resolved with remote config off
        // (customEntitlementComputation), so a call here is a wiring bug worth surfacing.
        if (manager.isDisabled) {
            errorLog { "ui_config is unavailable: remote config is disabled for this SDK configuration." }
            return UiConfigResolution.Unavailable
        }
        val topic = manager.committedTopicOrNull(RemoteConfigTopic.UiConfig)
        val presentKeys = ITEM_KEYS.filter { topic?.containsKey(it) == true }
        return if (presentKeys.isEmpty()) {
            verboseLog {
                val state = if (topic == null) "is absent" else "carries no ui_config part"
                "The ui_config topic $state; nothing to resolve."
            }
            UiConfigResolution.NotConfigured
        } else {
            verboseLog {
                "The ui_config topic carries ${presentKeys.size} of ${ITEM_KEYS.size} part(s) $presentKeys, " +
                    "but they could not be resolved into a UiConfig."
            }
            UiConfigResolution.Unavailable
        }
    }

    private companion object {
        private val ITEM_KEYS = listOf("app", "localizations", "variable_config", "custom_variables")
    }
}
