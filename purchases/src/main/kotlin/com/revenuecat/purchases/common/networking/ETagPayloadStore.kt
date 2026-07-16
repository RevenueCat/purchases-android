package com.revenuecat.purchases.common.networking

import android.content.Context
import androidx.core.util.AtomicFile
import com.revenuecat.purchases.common.errorLog
import com.revenuecat.purchases.models.toHexString
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.CharBuffer
import java.nio.charset.CodingErrorAction
import java.security.MessageDigest

/**
 * Stores ETag cache payloads as one file per URL under a cache directory, so caching a multi-MB response
 * costs neither JSON re-encoding nor permanent heap retention (SharedPreferences keeps its whole file parsed
 * in memory for the process lifetime; see https://github.com/RevenueCat/purchases-android/issues/3628).
 *
 * Writes are atomic and crash-safe via [AtomicFile] and encode straight from the payload string into a
 * fixed-size buffer, so storing never allocates payload-sized memory. Reads return the payload string, or
 * `null` for a missing/unreadable file, which callers treat as a cache miss ([ETagManager]'s existing
 * self-heal contract). Living under [Context.getCacheDir] means the system may purge files under storage
 * pressure; that also reads back as a miss.
 *
 * No eviction beyond per-URL overwrite and [clear] — parity with the SharedPreferences store it replaces.
 */
internal class ETagPayloadStore(
    private val directory: File,
) {
    constructor(context: Context) : this(File(context.cacheDir, DIRECTORY_NAME))

    /** Returns `true` once the payload is durably on disk; callers must not persist metadata otherwise. */
    @Suppress("ReturnCount")
    fun write(urlString: String, payload: String): Boolean {
        if (!directory.exists() && !directory.mkdirs()) {
            errorLog { "Failed to create ETag payload directory: $directory" }
            return false
        }
        val atomicFile = AtomicFile(fileFor(urlString))
        return try {
            val out = atomicFile.startWrite()
            try {
                encodeTo(out, payload)
                atomicFile.finishWrite(out)
            } catch (e: IOException) {
                atomicFile.failWrite(out)
                throw e
            }
            true
        } catch (e: IOException) {
            errorLog(e) { "Failed to persist ETag payload to disk." }
            false
        }
    }

    @Suppress("SwallowedException")
    fun read(urlString: String): String? {
        val atomicFile = AtomicFile(fileFor(urlString))
        return try {
            String(atomicFile.readFully(), Charsets.UTF_8)
        } catch (e: FileNotFoundException) {
            // No payload for this URL — a plain cache miss, not an error.
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
        val hash = MessageDigest.getInstance("SHA-256").digest(urlString.toByteArray()).toHexString()
        return File(directory, hash)
    }

    /**
     * UTF-8 encodes [payload] into [out] through a fixed-size buffer. A plain `Writer.write(String)` copies
     * the whole string into a fresh `char[]` first — a payload-sized allocation, the exact cost this store
     * exists to avoid — while `CharBuffer.wrap` reads the string in place.
     */
    private fun encodeTo(out: OutputStream, payload: String) {
        val encoder = Charsets.UTF_8.newEncoder()
            .onMalformedInput(CodingErrorAction.REPLACE)
            .onUnmappableCharacter(CodingErrorAction.REPLACE)
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
        const val WRITE_BUFFER_BYTES = 64 * 1024
    }
}
