package com.revenuecat.purchases.common.remoteconfig

import android.content.Context
import com.revenuecat.purchases.common.errorLog
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.IOException

/**
 * Persists the latest successful [RemoteConfigResponse] to disk under
 * `noBackupFilesDir/RevenueCat/remote_config.json`.
 *
 * This is currently write-only. A subsequent PR will read this file at startup to seed the
 * in-memory cache before the first network refresh.
 */
internal class RemoteConfigDiskCache(
    private val applicationContext: Context,
    private val json: Json,
) {
    fun write(response: RemoteConfigResponse) {
        val parent = File(applicationContext.noBackupFilesDir, REMOTE_CONFIG_ROOT)
        try {
            if (!parent.exists()) {
                parent.mkdirs()
            }
            val target = File(parent, REMOTE_CONFIG_FILE_NAME)
            val tempFile = File.createTempFile("rc_remote_config_", ".tmp", parent)
            try {
                tempFile.writeText(json.encodeToString(response))
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
        const val REMOTE_CONFIG_ROOT = "RevenueCat"
        const val REMOTE_CONFIG_FILE_NAME = "remote_config.json"
    }
}
