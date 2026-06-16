package com.revenuecat.purchases.common.remoteconfig

import android.content.Context
import com.revenuecat.purchases.common.JsonProvider
import com.revenuecat.purchases.common.errorLog
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import java.io.File
import java.io.IOException

/** The last server [manifest] plus the resolved topic bodies the SDK currently has cached. */
@Serializable
internal data class PersistedRemoteConfig(
    val manifest: RemoteConfiguration.Manifest,
    val topics: Map<String, ConfigTopic> = emptyMap(),
)

/**
 * Persists [PersistedRemoteConfig] to `noBackupFilesDir/RevenueCat/` (excluded from backups as a regenerable
 * cache). Writes are atomic (temp file + rename); a missing or corrupt file reads back as `null`.
 */
internal class RemoteConfigDiskCache(
    private val applicationContext: Context,
) {
    private val json = JsonProvider.defaultJson

    fun read(): PersistedRemoteConfig? {
        val target = File(File(applicationContext.noBackupFilesDir, REMOTE_CONFIG_ROOT), REMOTE_CONFIG_FILE_NAME)
        if (!target.exists()) return null
        return try {
            json.decodeFromString(PersistedRemoteConfig.serializer(), target.readText())
        } catch (e: IOException) {
            errorLog(e) { "Failed to read remote config from disk." }
            null
        } catch (e: SerializationException) {
            errorLog(e) { "Failed to deserialize remote config from disk." }
            null
        }
    }

    fun write(manifest: RemoteConfiguration.Manifest, resolvedTopics: Map<String, ConfigTopic>) {
        val parent = File(applicationContext.noBackupFilesDir, REMOTE_CONFIG_ROOT)
        try {
            if (!parent.exists()) {
                parent.mkdirs()
            }
            val content = json.encodeToString(
                PersistedRemoteConfig.serializer(),
                PersistedRemoteConfig(manifest, resolvedTopics),
            )
            val target = File(parent, REMOTE_CONFIG_FILE_NAME)
            val tempFile = File.createTempFile("rc_remote_config_", ".tmp", parent)
            try {
                tempFile.writeText(content)
                if (!tempFile.renameTo(target)) {
                    tempFile.copyTo(target, overwrite = true)
                }
            } finally {
                if (tempFile.exists()) {
                    tempFile.delete()
                }
            }
        } catch (e: IOException) {
            errorLog(e) { "Failed to persist remote config to disk." }
        } catch (e: SerializationException) {
            errorLog(e) { "Failed to serialize remote config for disk persistence." }
        }
    }

    private companion object {
        private const val REMOTE_CONFIG_ROOT = "RevenueCat"
        private const val REMOTE_CONFIG_FILE_NAME = "remote_config.json"
    }
}
