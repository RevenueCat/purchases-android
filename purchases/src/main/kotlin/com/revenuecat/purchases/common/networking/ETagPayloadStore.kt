package com.revenuecat.purchases.common.networking

import android.content.Context
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.common.errorLog
import com.revenuecat.purchases.models.Checksum
import com.revenuecat.purchases.models.toHexString
import java.io.DataInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.CharBuffer
import java.nio.charset.CoderResult
import java.nio.charset.CodingErrorAction
import java.security.DigestOutputStream
import java.security.MessageDigest

@OptIn(InternalRevenueCatAPI::class)
internal data class ETagPayloadInfo(
    val sizeBytes: Long,
    val checksum: Checksum,
)

/**
 * Stores ETag cache payloads as one file per URL, so multi-MB payloads are neither re-encoded nor
 * retained in the SharedPreferences heap map (https://github.com/RevenueCat/purchases-android/issues/3628).
 *
 * Writes stream through fixed-size buffers into a temp file committed by atomic rename (androidx
 * AtomicFile is unsafe here: its openRead deletes a concurrent writer's in-flight file and its
 * finishWrite hides rename failures). Writes are not fsynced; callers verify the size and SHA-256
 * [write] returns on [read] instead, turning truncated, stale, or corrupt files into misses.
 *
 * `null` reads are cache misses that self-heal via [ETagManager]'s refresh retry; the OS may purge
 * [Context.getCacheDir]. No eviction beyond overwrite and [clear], at parity with the prefs store.
 */
@OptIn(InternalRevenueCatAPI::class)
internal class ETagPayloadStore(
    private val directory: File,
) {
    constructor(context: Context) : this(File(File(context.cacheDir, VENDOR_DIRECTORY), DIRECTORY_NAME))

    /**
     * Returns the payload's integrity metadata once the file is in place, or `null` when the write
     * failed; callers must not persist metadata for a failed write.
     */
    fun write(urlString: String, payload: String): ETagPayloadInfo? {
        if (!directory.exists()) {
            deleteTrash()
            if (!directory.mkdirs()) {
                errorLog { "Failed to create ETag payload directory: $directory" }
                return null
            }
        }
        val file = fileFor(urlString)
        // Never opened by readers; a crash mid-write leaves one orphan that the next write overwrites.
        // Same-URL writes are serialized by [ETagManager].
        val tempFile = File(directory, file.name + TEMP_SUFFIX)
        return try {
            val digest = MessageDigest.getInstance(Checksum.Algorithm.SHA256.algorithmName)
            FileOutputStream(tempFile).use { fileOut ->
                DigestOutputStream(fileOut, digest).use { digestOut ->
                    encodeTo(digestOut, payload)
                }
            }
            val info = ETagPayloadInfo(
                sizeBytes = tempFile.length(),
                checksum = Checksum(
                    Checksum.Algorithm.SHA256,
                    digest.digest().toHexString(),
                ),
            )
            if (tempFile.renameTo(file)) info else null
        } catch (e: IOException) {
            errorLog(e) { "Failed to persist ETag payload to disk." }
            null
        }
    }

    /**
     * Returns the payload, or `null` for a miss: no file or an integrity mismatch against
     * [expectedInfo] (the metadata [write] returned).
     */
    @Suppress("SwallowedException", "ReturnCount")
    fun read(urlString: String, expectedInfo: ETagPayloadInfo): String? {
        return try {
            FileInputStream(fileFor(urlString)).use { input ->
                // The open descriptor pins one file identity, so a concurrent rename cannot swap the
                // payload between the size check and the read.
                val sizeBytes = input.channel.size()
                val size = sizeBytes.toInt()
                if (sizeBytes != expectedInfo.sizeBytes || size.toLong() != sizeBytes) {
                    return null
                }
                val bytes = ByteArray(size)
                DataInputStream(input).readFully(bytes)
                val actualChecksum = Checksum.generate(bytes, Checksum.Algorithm.SHA256)
                if (actualChecksum != expectedInfo.checksum) return null
                // The checksum proves these bytes are identical to the strictly encoded write, so direct
                // construction is safe and avoids the decoder's full UTF-16 CharBuffer allocation.
                String(bytes, Charsets.UTF_8)
            }
        } catch (e: FileNotFoundException) {
            // No payload for this URL: a plain cache miss, not an error.
            null
        } catch (e: IOException) {
            errorLog(e) { "Failed to read ETag payload from disk." }
            null
        }
    }

    fun clear() {
        // Renaming is a constant-time metadata op, safe on the main thread (configure, logIn/logOut);
        // the trashed directory is deleted by the next write, which runs on a background thread.
        if (!directory.exists()) return
        val trash = File(directory.parentFile, directory.name + TRASH_SUFFIX + System.nanoTime())
        if (!directory.renameTo(trash)) {
            directory.deleteRecursively()
        }
    }

    private fun deleteTrash() {
        directory.parentFile
            ?.listFiles { file -> file.name.startsWith(directory.name + TRASH_SUFFIX) }
            ?.forEach { it.deleteRecursively() }
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
        const val VENDOR_DIRECTORY = "RevenueCat"
        const val DIRECTORY_NAME = "etag_payloads"
        const val TEMP_SUFFIX = ".tmp"
        const val TRASH_SUFFIX = ".trash"
        const val CHUNK_CHARS = 64 * 1024
        const val WRITE_BUFFER_BYTES = 256 * 1024
    }
}
