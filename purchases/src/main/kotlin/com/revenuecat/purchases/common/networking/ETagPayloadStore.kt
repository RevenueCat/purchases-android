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
import java.nio.charset.CoderResult
import java.nio.charset.CodingErrorAction

/**
 * Stores ETag cache payloads as one file per URL, so multi-MB payloads are neither re-encoded nor
 * retained in the SharedPreferences heap map (https://github.com/RevenueCat/purchases-android/issues/3628).
 *
 * Writes stream through fixed-size buffers into a temp file committed by atomic rename (androidx
 * AtomicFile is unsafe here: its openRead deletes a concurrent writer's in-flight file and its
 * finishWrite hides rename failures). Writes are not fsynced; callers verify the size [write] returns
 * on [read] instead, turning files truncated by power loss into misses. A stale same-length file is
 * accepted: it serves the previous complete payload.
 *
 * `null` reads are cache misses that self-heal via [ETagManager]'s refresh retry; the OS may purge
 * [Context.getCacheDir]. No eviction beyond overwrite and [clear], at parity with the prefs store.
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
        // Never opened by readers; a crash mid-write leaves one orphan that the next write overwrites.
        // Same-URL writes are serialized by [ETagManager].
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
     * Returns the payload, or `null` for a miss: no file, undecodable bytes, or a size mismatch
     * against [expectedSizeBytes] (the size [write] returned; `null` skips the check).
     */
    @Suppress("SwallowedException")
    fun read(urlString: String, expectedSizeBytes: Long? = null): String? {
        val file = fileFor(urlString)
        return try {
            if (expectedSizeBytes != null && file.length() != expectedSizeBytes) {
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
     * Streams [payload] to [out] via a `char[]` chunk: `CharBuffer.wrap(payload)` has no backing array,
     * which forces the encoder off its fast path and measured a >60x slower store on ART. REPORT so an
     * unencodable payload fails the write instead of being silently altered.
     */
    private fun encodeTo(out: OutputStream, payload: String) {
        val encoder = Charsets.UTF_8.newEncoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
        val chunk = CharArray(CHUNK_CHARS)
        val charBuffer = CharBuffer.wrap(chunk)
        // Sized so a full chunk always encodes in one pass (UTF-8 is at most 3 bytes per UTF-16 char).
        val byteBuffer = ByteBuffer.allocate(WRITE_BUFFER_BYTES)
        var payloadPosition = 0
        // Chars the encoder left unconsumed at a chunk edge (a high surrogate whose pair is in the next
        // chunk); carried to the front of the next chunk so pairs are never encoded split.
        var carriedChars = 0
        do {
            val charsToCopy = minOf(chunk.size - carriedChars, payload.length - payloadPosition)
            payload.toCharArray(chunk, carriedChars, payloadPosition, payloadPosition + charsToCopy)
            payloadPosition += charsToCopy
            val isLastChunk = payloadPosition == payload.length
            charBuffer.position(0)
            charBuffer.limit(carriedChars + charsToCopy)
            drainInto(out, byteBuffer) { encoder.encode(charBuffer, byteBuffer, isLastChunk) }
            carriedChars = charBuffer.remaining()
            if (carriedChars > 0) {
                System.arraycopy(chunk, charBuffer.position(), chunk, 0, carriedChars)
            }
        } while (payloadPosition < payload.length)
        drainInto(out, byteBuffer) { encoder.flush(byteBuffer) }
    }

    /** Runs [step] until it reports underflow, writing whatever it put in [byteBuffer] after each round. */
    private inline fun drainInto(out: OutputStream, byteBuffer: ByteBuffer, step: () -> CoderResult) {
        while (true) {
            val result = step()
            if (result.isError) result.throwException()
            out.write(byteBuffer.array(), 0, byteBuffer.position())
            byteBuffer.clear()
            if (result.isUnderflow) return
        }
    }

    private companion object {
        const val DIRECTORY_NAME = "rc_etag_payloads"
        const val TEMP_SUFFIX = ".tmp"
        const val CHUNK_CHARS = 64 * 1024
        const val WRITE_BUFFER_BYTES = 256 * 1024
    }
}
