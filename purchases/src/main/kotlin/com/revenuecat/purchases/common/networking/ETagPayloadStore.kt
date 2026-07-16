package com.revenuecat.purchases.common.networking

import android.content.Context
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.common.errorLog
import com.revenuecat.purchases.models.Checksum
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.CharBuffer
import java.nio.charset.CodingErrorAction

/**
 * Stores ETag cache payloads as one file per URL under a cache directory, so caching a multi-MB response
 * costs neither JSON re-encoding nor permanent heap retention (SharedPreferences keeps its whole file parsed
 * in memory for the process lifetime; see https://github.com/RevenueCat/purchases-android/issues/3628).
 *
 * Writes encode straight from the payload string into a fixed-size buffer, so storing never allocates
 * payload-sized memory. Each write goes to a temporary file that is atomically renamed over the final one,
 * so readers only ever see complete payloads: a read concurrent with a write serves the previous complete
 * payload. (androidx AtomicFile is deliberately not used: its openRead mutates the filesystem, deleting a
 * concurrent writer's in-flight file, and its finishWrite reports rename failures only to logcat.)
 *
 * Writes are deliberately not fsynced, keeping disk latency off the request path. A power loss shortly
 * after a write can therefore leave a truncated file behind; callers guard against that by recording the
 * size [write] returns and passing it back to [read], which verifies it against the file. (The other
 * power-loss residue, a stale same-length file behind fresher metadata, is accepted: it is the complete
 * previous payload and simply serves as a stale response until the content next changes.)
 *
 * Reads return the payload string, or `null` for a missing/truncated/unreadable file, which callers treat
 * as a cache miss ([ETagManager]'s existing self-heal contract). Living under [Context.getCacheDir] means
 * the system may purge files under storage pressure; that also reads back as a miss.
 *
 * No eviction beyond per-URL overwrite and [clear]: parity with the SharedPreferences store it replaces.
 */
@OptIn(InternalRevenueCatAPI::class)
internal class ETagPayloadStore(
    private val directory: File,
) {
    constructor(context: Context) : this(File(context.cacheDir, DIRECTORY_NAME))

    /**
     * Returns the payload's size in bytes once the file is in place, or `null` when the write failed;
     * callers must not persist metadata for a failed write.
     */
    fun write(urlString: String, payload: String): Long? {
        if (!directory.exists() && !directory.mkdirs()) {
            errorLog { "Failed to create ETag payload directory: $directory" }
            return null
        }
        val file = fileFor(urlString)
        // One temp file per target, overwritten by the next write and removed by the rename on success,
        // so a crash mid-write leaves at most one orphan that self-cleans. Writes for the same URL never
        // run concurrently ([ETagManager] serializes them), and readers never open temp files.
        val tempFile = File(directory, file.name + TEMP_SUFFIX)
        return try {
            FileOutputStream(tempFile).use { out -> encodeTo(out, payload) }
            if (tempFile.renameTo(file)) file.length() else null
        } catch (e: IOException) {
            errorLog(e) { "Failed to persist ETag payload to disk." }
            null
        }
    }

    /**
     * Returns the payload for [urlString], or `null` for a miss: no file, a size mismatch against
     * [expectedSizeBytes] (pass the size [write] returned; `null` skips the check), or undecodable bytes.
     */
    @Suppress("SwallowedException")
    fun read(urlString: String, expectedSizeBytes: Long? = null): String? {
        val file = fileFor(urlString)
        return try {
            if (expectedSizeBytes != null && file.length() != expectedSizeBytes) {
                // Truncated by a power loss (writes are not fsynced) or otherwise tampered with.
                return null
            }
            // Strict decoding (unlike String(bytes), which silently substitutes U+FFFD) so a corrupt file
            // reads back as a cache miss instead of serving garbage as a valid cached response.
            Charsets.UTF_8.newDecoder()
                .decode(ByteBuffer.wrap(file.readBytes()))
                .toString()
        } catch (e: FileNotFoundException) {
            // No payload for this URL: a plain cache miss, not an error.
            null
        } catch (e: IOException) {
            errorLog(e) { "Failed to read ETag payload from disk." }
            null
        }
    }

    fun clear() {
        directory.deleteRecursively()
    }

    private fun fileFor(urlString: String): File {
        return File(directory, Checksum.generate(urlString.toByteArray(), Checksum.Algorithm.SHA256).value)
    }

    /**
     * UTF-8 encodes [payload] into [out] through a fixed-size buffer. A plain `Writer.write(String)`
     * copies the whole string into a fresh `char[]` first, a payload-sized allocation, the exact cost
     * this store exists to avoid; `CharBuffer.wrap` reads the string in place. Encoding is strict
     * (REPORT, matching [read]'s decoder): a payload the encoder cannot represent fails the write and
     * stays uncached, rather than being silently altered.
     */
    private fun encodeTo(out: OutputStream, payload: String) {
        val encoder = Charsets.UTF_8.newEncoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
        val charBuffer = CharBuffer.wrap(payload)
        val byteBuffer = ByteBuffer.allocate(WRITE_BUFFER_BYTES)
        var encoded = false
        while (true) {
            val result = if (!encoded) encoder.encode(charBuffer, byteBuffer, true) else encoder.flush(byteBuffer)
            if (result.isError) result.throwException()
            out.write(byteBuffer.array(), 0, byteBuffer.position())
            byteBuffer.clear()
            if (result.isUnderflow) {
                if (encoded) break
                encoded = true
            }
        }
    }

    private companion object {
        const val DIRECTORY_NAME = "rc_etag_payloads"
        const val TEMP_SUFFIX = ".tmp"
        const val WRITE_BUFFER_BYTES = 64 * 1024
    }
}
