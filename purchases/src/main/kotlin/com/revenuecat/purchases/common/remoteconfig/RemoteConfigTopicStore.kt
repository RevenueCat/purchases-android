package com.revenuecat.purchases.common.remoteconfig

/**
 * Thin read facade over [RemoteConfigDiskCache] exposing the committed per-topic item index.
 *
 * Topic data is committed as the source of truth (the full [ConfigTopic] item index, metadata only — the
 * heavy blob bytes live in [RemoteConfigBlobStore]), so reads here are cheap metadata lookups with no blob
 * resolution and no wait. Lazy read-side collaborators (e.g. the source provider) depend on this rather than
 * on the disk cache directly.
 *
 * Phase 5 introduces the minimal surface the source provider needs ([read]/[topic]); later phases extend it
 * with the consumer `body()` resolution.
 */
internal class RemoteConfigTopicStore(private val diskCache: RemoteConfigDiskCache) {

    /** The whole persisted state, or null if nothing has been committed yet / the file is unreadable. */
    fun read(): PersistedRemoteConfigurationState? = diskCache.read()

    /** The committed item index for [name], or null if the topic is absent / not yet synced. */
    fun topic(name: String): ConfigTopic? = read()?.topics?.get(name)
}
