package com.revenuecat.purchases.common.remoteconfig

import android.content.Context
import com.revenuecat.purchases.common.errorLog
import java.io.File
import java.io.IOException
import java.nio.ByteBuffer

/**
 * A content-addressed, on-disk cache for remote-config blobs. Each blob is stored as one file under
 * `noBackupFilesDir/RevenueCat/blobs/` (excluded from backups as a regenerable cache), named by its ref: the
 * URL-safe base64 of the blob's truncated SHA-256, matching `RCElement.checksumBase64`.
 *
 * The ref encoding (`A-Z a-z 0-9 - _`, no padding, fixed 32 chars for a 24-byte hash) is filename-safe, so the
 * ref doubles as the filename. Refs that don't match that shape are rejected so a malformed ref can never
 * escape the blobs directory.
 *
 * Every operation is forgiving: IO failures are logged and swallowed, and a missing or unreadable blob reads
 * back as `null`, mirroring [RemoteConfigDiskCache].
 */
internal class RemoteConfigBlobStore(
    private val applicationContext: Context,
) {
    fun contains(ref: String): Boolean = blobFile(ref)?.exists() == true

    fun read(ref: String): ByteArray? {
        val target = blobFile(ref)?.takeIf { it.exists() } ?: return null
        return try {
            target.readBytes()
        } catch (e: IOException) {
            errorLog(e) { "Failed to read remote config blob '$ref' from disk." }
            null
        }
    }

    /** Returns whether the blob was actually persisted; IO failures are logged, swallowed, and reported as `false`. */
    fun write(ref: String, data: ByteBuffer): Boolean {
        val target = blobFile(ref)
        if (target == null) {
            errorLog { "Refusing to write remote config blob with malformed ref '$ref'." }
            return false
        }
        val parent = blobsDir()
        return try {
            if (!parent.exists()) {
                parent.mkdirs()
            }
            val view = data.duplicate()
            val bytes = ByteArray(view.remaining())
            view.get(bytes)
            val tempFile = File.createTempFile("rc_blob_", ".tmp", parent)
            try {
                tempFile.writeBytes(bytes)
                if (!tempFile.renameTo(target)) {
                    tempFile.copyTo(target, overwrite = true)
                }
            } finally {
                if (tempFile.exists()) {
                    tempFile.delete()
                }
            }
            true
        } catch (e: IOException) {
            errorLog(e) { "Failed to persist remote config blob '$ref' to disk." }
            false
        }
    }

    /** The refs currently cached on disk (only files whose name is a valid ref). */
    fun cachedRefs(): Set<String> {
        val parent = blobsDir()
        if (!parent.exists()) return emptySet()
        return parent.listFiles()
            ?.asSequence()
            ?.filter { it.isFile && RemoteConfigUtils.isValidRef(it.name) }
            ?.map { it.name }
            ?.toSet()
            ?: emptySet()
    }

    /** Deletes every cached blob whose ref is not in [refs]. */
    fun retainOnly(refs: Set<String>) {
        val parent = blobsDir()
        if (!parent.exists()) return
        parent.listFiles()
            ?.filter { it.isFile && it.name !in refs }
            ?.forEach { deleteQuietly(it) }
    }

    /**
     * Deletes every cached blob unconditionally. Used on identity change to keep config from bleeding across
     * users (unlike [retainOnly], which only prunes unreferenced blobs).
     */
    fun clear() {
        val parent = blobsDir()
        if (!parent.exists()) return
        parent.listFiles()?.forEach { deleteQuietly(it) }
    }

    private fun deleteQuietly(file: File) {
        try {
            if (!file.delete()) {
                errorLog { "Failed to delete unreferenced remote config blob '${file.name}'." }
            }
        } catch (e: SecurityException) {
            errorLog(e) { "Failed to delete unreferenced remote config blob '${file.name}'." }
        }
    }

    private fun blobsDir(): File =
        File(File(applicationContext.noBackupFilesDir, REMOTE_CONFIG_ROOT), BLOBS_DIR)

    /** The target file for [ref], or `null` when [ref] is not a valid content-address ref. */
    private fun blobFile(ref: String): File? =
        if (RemoteConfigUtils.isValidRef(ref)) File(blobsDir(), ref) else null

    private companion object {
        private const val REMOTE_CONFIG_ROOT = "RevenueCat"
        private const val BLOBS_DIR = "blobs"
    }
}
