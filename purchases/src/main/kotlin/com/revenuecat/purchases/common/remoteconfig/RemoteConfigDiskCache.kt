package com.revenuecat.purchases.common.remoteconfig

import android.content.Context
import androidx.core.util.AtomicFile
import com.revenuecat.purchases.common.JsonProvider
import com.revenuecat.purchases.common.errorLog
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import java.io.File
import java.io.IOException

/**
 * One domain's sync bookkeeping between `/v1/config` calls.
 *
 * [manifest] is the **opaque** server token, stored verbatim and replayed on the next request for this domain;
 * the SDK never parses it. [subdomains] is the last response's list of other domains that also contribute to the
 * full configuration (outside the manifest, so it does not participate in change detection). [activeTopics] is
 * the last response's full active-topic-name set (used to detect removed topics). [prefetchBlobs] is the last
 * response's prefetch set (retention input + which blobs to fetch).
 *
 * [topics] is the full per-topic item index — the configuration itself (each item's `blob_ref` plus its inline
 * `content`), which is the **source of truth**: persisting it is the whole sync commit, and consumers read their
 * topic metadata back from it. Only the heavy blob *bytes* live elsewhere (the content-addressed blob store,
 * keyed by `blob_ref`); the index holds the small metadata map per topic (an empty map for inline-only topics).
 *
 * [lastRefreshTime] is the epoch-millis instant of this domain's last successful config request — a `200` *or* a
 * `204`, since both confirm the cached configuration is current. It is replayed in the `X-RC-Last-Refresh-Time`
 * request header so the server can optimize its response, which is why it is persisted next to the [manifest] it
 * pairs with: an app-start request must carry it too. Unlike the topic index it is recoverable metadata, not
 * source of truth, so a failed write only costs the next request a staler value.
 *
 * The value is always the **server's** own `X-RevenueCat-Request-Time`, never a device-clock reading: the server
 * compares it against its own clock, so a skewed device would silently corrupt that comparison. A response without
 * the header leaves the previous value in place rather than substituting local time.
 */
@Serializable
internal data class PersistedDomainState(
    val manifest: String,
    val subdomains: List<String> = emptyList(),
    val activeTopics: List<String> = emptyList(),
    val prefetchBlobs: List<String> = emptyList(),
    val topics: Map<String, ConfigTopic> = emptyMap(),
    val lastRefreshTime: Long? = null,
)

/**
 * The full persisted configuration: every synced domain's [PersistedDomainState], keyed by domain name, plus
 * which domain is the tree's root. Kept as a **single atomic write** (a sync commit updates one domain's entry
 * but always rewrites the whole file), so the persisted tree is never internally torn.
 *
 * The merged views ([domainsInPrecedenceOrder], [mergedTopics], [liveBlobRefs]) are derived lazily per state
 * instance and cached — the state is immutable and the disk cache hands out one snapshot, so they compute at
 * most once per committed state even though the topic store reads on every HTTP request.
 */
@Serializable
internal data class PersistedRemoteConfigurationState(
    val rootDomain: String,
    val domains: Map<String, PersistedDomainState> = emptyMap(),
) {
    val rootState: PersistedDomainState?
        get() = domains[rootDomain]

    /**
     * Every persisted domain reachable from [rootDomain] through the persisted `subdomains` lists, root-first
     * BFS (shallower domains before deeper ones, siblings in declaration order), guarded by a visited set
     * (cycles/duplicates contribute once) and [MAX_SUBDOMAIN_DEPTH]. Domains present in [domains] but not
     * reachable from the root are excluded: they are stale entries awaiting pruning, never part of the merged
     * configuration.
     */
    val domainsInPrecedenceOrder: List<String> by lazy {
        buildList {
            val visited = mutableSetOf<String>()
            var frontier = listOf(rootDomain)
            var depth = 0
            while (frontier.isNotEmpty() && depth <= MAX_SUBDOMAIN_DEPTH) {
                val level = frontier.filter { visited.add(it) }.filter { it in domains }
                addAll(level)
                frontier = level.flatMap { domains.getValue(it).subdomains }
                depth++
            }
        }
    }

    /**
     * The union of every reachable domain's topics — the configuration consumers read. On a topic-name
     * collision the domain earlier in [domainsInPrecedenceOrder] wins (parent over child, so a topic migrating
     * to a subdomain cuts over exactly when its parent stops serving it); topics are never merged item-wise
     * across domains.
     */
    val mergedTopics: Map<String, ConfigTopic> by lazy {
        buildMap {
            domainsInPrecedenceOrder.forEach { domain ->
                domains[domain]?.topics?.forEach { (name, topic) -> putIfAbsent(name, topic) }
            }
        }
    }

    /**
     * Every blob ref any persisted domain still wants (its prefetch set plus its topics' `blob_ref`s) — the
     * retention set for blob-store cleanup. Deliberately spans **all** entries in [domains], including ones not
     * reachable from the root: an entry that survived a partial sync keeps pinning its blobs until the entry
     * itself is pruned, so cleanup can never race ahead of the state that references the blobs.
     */
    val liveBlobRefs: Set<String> by lazy {
        buildSet {
            domains.values.forEach { state ->
                addAll(state.prefetchBlobs)
                addAll(state.topics.toTopicBlobRefs().values.flatten())
            }
        }
    }

    companion object {
        /** Root sits at depth 0; subdomain lists deeper than this are ignored (logged at sync time). */
        const val MAX_SUBDOMAIN_DEPTH = 3
    }
}

/**
 * The pre-subdomain persisted shape (a single flat domain). Kept only so an existing file written by an older
 * SDK version lifts into [PersistedRemoteConfigurationState] instead of forcing a cold full re-fetch.
 */
@Serializable
private data class LegacyPersistedRemoteConfigurationState(
    val domain: String,
    val manifest: String,
    val activeTopics: List<String> = emptyList(),
    val prefetchBlobs: List<String> = emptyList(),
    val topics: Map<String, ConfigTopic> = emptyMap(),
    val lastRefreshTime: Long? = null,
) {
    fun migrated() = PersistedRemoteConfigurationState(
        rootDomain = domain,
        domains = mapOf(
            domain to PersistedDomainState(
                manifest = manifest,
                activeTopics = activeTopics,
                prefetchBlobs = prefetchBlobs,
                topics = topics,
                lastRefreshTime = lastRefreshTime,
            ),
        ),
    )
}

/**
 * Persists [PersistedRemoteConfigurationState] to `noBackupFilesDir/RevenueCat/remote_config/`
 * (excluded from backups as a regenerable cache). Writes are atomic and crash-safe via [AtomicFile];
 * a missing or corrupt file reads back as `null`.
 */
internal class RemoteConfigDiskCache(
    private val applicationContext: Context,
) {
    private val json = JsonProvider.defaultJson
    private val lock = Any()

    // In-memory snapshot of the persisted state. `snapshotLoaded` distinguishes "not read yet" from
    // "read: nothing usable on disk (null)", so a missing/corrupt file is not re-read on every call.
    private var snapshot: PersistedRemoteConfigurationState? = null
    private var snapshotLoaded = false

    fun read(): PersistedRemoteConfigurationState? = synchronized(lock) {
        if (!snapshotLoaded) {
            snapshot = readFromDisk()
            snapshotLoaded = true
        }
        snapshot
    }

    /** Returns `true` once the state is durably persisted, `false` if serialization or IO failed. */
    fun write(config: PersistedRemoteConfigurationState): Boolean = synchronized(lock) {
        writeToDisk(config).also { persisted ->
            if (persisted) {
                // Only mirror a durable write; on failure the previous state is still what is on disk.
                snapshot = config
                snapshotLoaded = true
            }
        }
    }

    private fun readFromDisk(): PersistedRemoteConfigurationState? {
        val target = targetFile()
        if (!target.exists()) return null
        return try {
            decode(AtomicFile(target).readFully().toString(Charsets.UTF_8))
        } catch (e: IOException) {
            errorLog(e) { "Failed to read remote config from disk." }
            null
        }
    }

    private fun decode(content: String): PersistedRemoteConfigurationState? = try {
        json.decodeFromString(PersistedRemoteConfigurationState.serializer(), content)
    } catch (e: SerializationException) {
        // Not the current shape: an older SDK version may have written the pre-subdomain single-domain shape
        // (its missing `rootDomain` fails the decode above). Lifting it preserves the manifest, so the next
        // sync is an incremental diff instead of a cold re-fetch. Migrated in memory only; the next successful
        // write persists the new shape.
        decodeLegacy(content) ?: run {
            errorLog(e) { "Failed to deserialize remote config from disk." }
            null
        }
    }

    private fun decodeLegacy(content: String): PersistedRemoteConfigurationState? = try {
        json.decodeFromString(LegacyPersistedRemoteConfigurationState.serializer(), content).migrated()
    } catch (_: SerializationException) {
        null
    }

    private fun writeToDisk(config: PersistedRemoteConfigurationState): Boolean {
        return try {
            val target = targetFile()
            target.parentFile?.let { parent ->
                if (!parent.exists()) {
                    parent.mkdirs()
                }
            }
            val content = json.encodeToString(
                PersistedRemoteConfigurationState.serializer(),
                config,
            )
            val atomicFile = AtomicFile(target)
            val out = atomicFile.startWrite()
            try {
                out.write(content.toByteArray())
                atomicFile.finishWrite(out)
            } catch (e: IOException) {
                atomicFile.failWrite(out)
                throw e
            }
            true
        } catch (e: IOException) {
            errorLog(e) { "Failed to persist remote config to disk." }
            false
        } catch (e: SerializationException) {
            errorLog(e) { "Failed to serialize remote config for disk persistence." }
            false
        }
    }

    /**
     * Deletes the persisted state so the next sync starts fresh (no manifest -> full re-fetch). Used on identity
     * change to keep configuration from bleeding across users.
     */
    fun clear() {
        synchronized(lock) {
            snapshot = null
            snapshotLoaded = true
            try {
                AtomicFile(targetFile()).delete()
            } catch (e: SecurityException) {
                errorLog(e) { "Failed to clear remote config from disk." }
            }
        }
    }

    private fun targetFile(): File =
        File(
            File(File(applicationContext.noBackupFilesDir, REMOTE_CONFIG_ROOT), REMOTE_CONFIG_SUBDIR),
            REMOTE_CONFIG_FILE_NAME,
        )

    private companion object {
        private const val REMOTE_CONFIG_ROOT = "RevenueCat"
        private const val REMOTE_CONFIG_SUBDIR = "remote_config"
        private const val REMOTE_CONFIG_FILE_NAME = "remote_config.json"
    }
}
