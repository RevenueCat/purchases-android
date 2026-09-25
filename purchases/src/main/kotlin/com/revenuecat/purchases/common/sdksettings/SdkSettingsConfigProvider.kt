package com.revenuecat.purchases.common.sdksettings

import com.revenuecat.purchases.common.debugLog
import com.revenuecat.purchases.common.remoteconfig.GenerationGuardedCache
import com.revenuecat.purchases.common.remoteconfig.RemoteConfigCommitListener
import com.revenuecat.purchases.common.remoteconfig.RemoteConfigManager
import com.revenuecat.purchases.common.remoteconfig.RemoteConfigTopic
import com.revenuecat.purchases.common.remoteconfig.RemoteConfiguration
import com.revenuecat.purchases.common.remoteconfig.readConsistent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/** Notified from the provider's IO scope whenever the warmed [SdkSettings] differ from the ones last delivered. */
internal fun interface SdkSettingsListener {
    fun onSdkSettingsChanged(settings: SdkSettings)
}

/**
 * The topic-specific front door for `sdk_settings`: a single inline `default` item whose metadata is the
 * [SdkSettings] object (no blob).
 *
 * Memory-first: the settings are warmed into memory at configure (never syncing `/v1/config`) and re-warmed
 * on every config commit, so [cachedSettings] can be read synchronously. They are served only while warm at
 * the manager's *current* [RemoteConfigManager.configGeneration]; otherwise [cachedSettings] is `null` and a
 * consumer keeps its build-time value, while [getSettings] falls through to the config layer.
 */
internal class SdkSettingsConfigProvider(
    private val manager: RemoteConfigManager,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
) : RemoteConfigCommitListener {

    private val cache = GenerationGuardedCache<SdkSettings>()

    // The (generation, settings) last handed to a listener. Kept apart from [cache] so an identity-change
    // invalidation (which empties the cache) doesn't make the next warm re-deliver unchanged settings, and
    // guarded by generation so a slow older warm can't record over a newer delivery.
    private val deliveryLock = Any()
    private var lastDelivered: Pair<Int, SdkSettings>? = null

    @Volatile
    var listener: SdkSettingsListener? = null

    /** The warmed settings at the current config generation, or `null` when not warm. Never suspends or reads disk. */
    fun cachedSettings(): SdkSettings? = cache.cachedAtOrAbove(manager.configGeneration)

    /**
     * The committed settings, or [SdkSettings.DEFAULT] when the topic or its `default` item is absent. A cold
     * read happens under one config generation: [readConsistent] re-reads once if a commit races the read, and
     * gives up with the defaults if that read is superseded too.
     */
    suspend fun getSettings(): SdkSettings {
        cachedSettings()?.let { return it }
        return manager.readConsistent(what = { "the sdk_settings topic" }) { _ -> readSettings() }
            ?: SdkSettings.DEFAULT
    }

    /**
     * Best-effort populate of the in-memory cache from already-committed config, tagged with [generation]. No-op
     * (no `/v1/config` sync) while no config is committed yet, so a cold-disk init warm never triggers a network
     * config fetch. Once a config is committed, a missing topic or `default` item warms the defaults, so a listener
     * hears about the settings reverting when the backend stops serving them.
     */
    suspend fun warm(generation: Int) {
        if (cache.isWarmAtOrAbove(generation) || !manager.hasCommittedConfig()) return
        val settings = manager.committedTopicOrNull(RemoteConfigTopic.SdkSettings)?.get(ITEM_DEFAULT).toSettings()
        if (cache.store(generation, settings)) {
            listener?.let { deliverIfChanged(generation, settings, it) }
        }
    }

    private fun deliverIfChanged(generation: Int, settings: SdkSettings, listener: SdkSettingsListener) {
        val changed = synchronized(deliveryLock) {
            val previous = lastDelivered
            if (previous != null && generation < previous.first) {
                false
            } else {
                lastDelivered = generation to settings
                previous?.second != settings
            }
        }
        if (changed) {
            debugLog { "SDK settings changed: $settings." }
            listener.onSdkSettingsChanged(settings)
        }
    }

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

    private suspend fun readSettings(): SdkSettings =
        manager.topic(RemoteConfigTopic.SdkSettings)?.get(ITEM_DEFAULT).toSettings()

    private fun RemoteConfiguration.ConfigItem?.toSettings(): SdkSettings =
        this?.let { SdkSettings.parse(it.metadata) } ?: SdkSettings.DEFAULT

    private companion object {
        const val ITEM_DEFAULT = "default"
    }
}
