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
 * The sync bookkeeping the SDK persists between `/v1/config` calls.
 *
 * [manifest] is the **opaque** server token, stored verbatim and replayed on the next request; the SDK never
 * parses it. [activeTopics] is the last response's full active-topic-name set (used to detect removed topics).
 * [prefetchBlobs] is the last response's prefetch set (retention input + which blobs to fetch).
 *
 * [topics] is the full per-topic item index — the configuration itself (each item's `blob_ref` plus its inline
 * `content`), which is the **source of truth**: persisting it is the whole sync commit, and consumers read their
 * topic metadata back from it. Only the heavy blob *bytes* live elsewhere (the content-addressed blob store,
 * keyed by `blob_ref`); the index holds the small metadata map per topic (an empty map for inline-only topics).
 */
@Serializable
internal data class PersistedRemoteConfigurationState(
    val domain: String,
    val manifest: String,
    val activeTopics: List<String> = emptyList(),
    val prefetchBlobs: List<String> = emptyList(),
    val topics: Map<String, ConfigTopic> = emptyMap(),
)

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
        val atomicFile = AtomicFile(target)
        return try {
            json.decodeFromString(
                PersistedRemoteConfigurationState.serializer(),
                atomicFile.readFully().toString(Charsets.UTF_8),
            )
        } catch (e: IOException) {
            errorLog(e) { "Failed to read remote config from disk." }
            null
        } catch (e: SerializationException) {
            errorLog(e) { "Failed to deserialize remote config from disk." }
            null
        }
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
