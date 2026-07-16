package com.revenuecat.purchases.common.remoteconfig

import android.content.Context
import androidx.core.util.AtomicFile
import com.revenuecat.purchases.common.errorLog
import java.io.File
import java.io.IOException

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
    private val lock = Any()

    // Refs known to be on disk. Loaded once from a disk scan on first access, then kept in sync by write /
    // retainOnly / clear, so contains() and cachedRefs() answer from memory instead of stat-ing the disk each
    // call. The store is the sole writer of its directory, so this stays authoritative; if disk and index ever
    // diverge, read() self-heals by evicting the ref on a miss, so a stale "cached" flag can't strand a re-fetch.
    private var knownRefs: MutableSet<String>? = null

    fun contains(ref: String): Boolean = synchronized(lock) { loadedRefs().contains(ref) }

    fun read(ref: String): ByteArray? {
        val target = blobFile(ref)?.takeIf { it.exists() }
        if (target == null) {
            // Disk and index disagree (ref gone from under us): correct the index so a later
            // ensureDownloaded/prefetch re-fetches instead of trusting a stale "cached" flag.
            synchronized(lock) { knownRefs?.remove(ref) }
            return null
        }
        return try {
            target.readBytes()
        } catch (e: IOException) {
            errorLog(e) { "Failed to read remote config blob '$ref' from disk." }
            null
        }
    }

    /**
     * Returns whether the blob was actually persisted; IO failures are logged, swallowed, and reported as `false`.
     *
     * Writes are crash-safe via [AtomicFile] (mirroring [RemoteConfigDiskCache]): the bytes go to a `<ref>.new`
     * side file that is synced and renamed over the target only on success, so the blob file itself is never
     * partial. An interrupted write leaves only the `.new` orphan, whose name is not a valid ref — it is
     * invisible to the index scan and pruned by [retainOnly].
     */
    fun write(ref: String, data: ByteArray): Boolean {
        val target = blobFile(ref)
        if (target == null) {
            errorLog { "Refusing to write remote config blob with malformed ref '$ref'." }
            return false
        }
        return try {
            // startWrite creates the blobs directory when missing.
            val atomicFile = AtomicFile(target)
            val out = atomicFile.startWrite()
            try {
                out.write(data)
                atomicFile.finishWrite(out)
            } catch (e: IOException) {
                atomicFile.failWrite(out)
                throw e
            }
            synchronized(lock) { loadedRefs().add(ref) }
            true
        } catch (e: IOException) {
            errorLog(e) { "Failed to persist remote config blob '$ref' to disk." }
            false
        }
    }

    /** The refs currently cached on disk (only files whose name is a valid ref). */
    fun cachedRefs(): Set<String> = synchronized(lock) { loadedRefs().toSet() }

    /**
     * Deletes every cached blob whose ref is not in [refs]. Scans the directory (not just the in-memory index)
     * so orphans the index never tracks are pruned too: leftover `<ref>.new` side files from an [AtomicFile]
     * write interrupted by a process kill, and any invalid-named file. Runs once per sync, so the scan is not
     * on a hot path.
     */
    fun retainOnly(refs: Set<String>) {
        val parent = blobsDir()
        if (parent.exists()) {
            parent.listFiles()
                ?.filter { it.isFile && it.name !in refs }
                ?.forEach { deleteQuietly(it) }
        }
        synchronized(lock) { loadedRefs().retainAll(refs) }
    }

    /**
     * Deletes every cached blob unconditionally. Used on identity change to keep config from bleeding across
     * users (unlike [retainOnly], which only prunes unreferenced blobs).
     */
    fun clear() {
        synchronized(lock) {
            val parent = blobsDir()
            if (parent.exists()) parent.listFiles()?.forEach { deleteQuietly(it) }
            knownRefs = mutableSetOf()
        }
    }

    /**
     * The in-memory ref index, populated on first access from a one-time scan of the blobs directory for files
     * whose name is a valid ref. Callers must hold [lock].
     */
    private fun loadedRefs(): MutableSet<String> = knownRefs ?: run {
        val parent = blobsDir()
        val scanned = parent.takeIf { it.exists() }
            ?.listFiles()
            ?.asSequence()
            ?.filter { it.isFile && RemoteConfigUtils.isValidRef(it.name) }
            ?.map { it.name }
            ?.toMutableSet()
            ?: mutableSetOf()
        scanned.also { knownRefs = it }
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
