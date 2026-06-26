package com.revenuecat.purchases.common.remoteconfig

import android.content.Context
import androidx.core.util.AtomicFile
import com.revenuecat.purchases.common.JsonProvider
import com.revenuecat.purchases.common.errorLog
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import java.io.File
import java.io.IOException

/**
 * The sync bookkeeping the SDK persists between `/v1/config` calls.
 *
 * [appUserID] scopes the manifest to the user whose request produced it. [manifest] is the **opaque** server
 * token, stored verbatim and replayed on the next request; the SDK never parses it. [activeTopics] is the last
 * response's full active-topic-name set (used to detect removed topics). [prefetchBlobs] is the last response's
 * prefetch set (retention input + which blobs to fetch). [lastRefreshAt] is the SDK's own wall-clock of the last
 * sync attempt, used only for refresh cadence.
 *
 * The full topic bodies are intentionally **not** persisted: an item's payload lives either in the blob store
 * (addressed by its blob ref) or is projected into a purpose-built cache by its topic handler, so the only
 * topic state the sync needs to keep is which blobs each topic keeps alive for retention. [topicBlobRefs] holds
 * an empty list for inline-only topics.
 */
@Serializable
internal data class PersistedRemoteConfigurationState(
    @SerialName("app_user_id") val appUserID: String? = null,
    val domain: String,
    val manifest: String,
    val activeTopics: List<String> = emptyList(),
    val prefetchBlobs: List<String> = emptyList(),
    val topicBlobRefs: Map<String, List<String>> = emptyMap(),
    val lastRefreshAt: Long = 0,
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

    fun read(): PersistedRemoteConfigurationState? {
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

    fun write(config: PersistedRemoteConfigurationState) {
        try {
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
        } catch (e: IOException) {
            errorLog(e) { "Failed to persist remote config to disk." }
        } catch (e: SerializationException) {
            errorLog(e) { "Failed to serialize remote config for disk persistence." }
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
