package com.revenuecat.purchases.common.caching

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.common.errorLog
import com.revenuecat.purchases.models.Checksum
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
import java.util.zip.CRC32
import java.util.zip.CheckedOutputStream

/**
 * Stores payloads as one file per key, so multi-MB payloads are neither re-encoded nor retained in the
 * SharedPreferences heap map.
 *
 * Writes stream through fixed-size buffers into a temp file committed by atomic rename (androidx
 * AtomicFile is unsafe here: its openRead deletes a concurrent writer's in-flight file and its
 * finishWrite hides rename failures). Callers verify the checksum [write] returns on [read], so a file
 * truncated by power loss, left stale by an interrupted rename, or corrupted in place all read back as misses.
 * Without [syncWrites] the temp file is not fsynced, and a filesystem can commit the rename before the data,
 * so a power loss can leave neither the old payload nor the new one.
 *
 * A [read] costs ~2x the payload size in heap (`ByteArray` plus `String`).
 *
 * `null` reads are cache misses. No eviction beyond overwrite, [delete] and [clear].
 */
@OptIn(InternalRevenueCatAPI::class)
internal class PayloadFileStore(
    private val directory: File,
    private val syncWrites: Boolean = false,
) {
    // Context storage directories are null on mocked contexts.
    constructor(baseDirectory: File?, name: String, syncWrites: Boolean = false) :
        this(File(File(baseDirectory, VENDOR_DIRECTORY), name), syncWrites)

    /**
     * Returns the payload's CRC32 once the file is in place, or `null` when the write failed; callers
     * must not persist metadata for a failed write.
     */
    fun write(key: String, payload: String): Long? {
        if (!directory.exists()) {
            deleteTrash()
            if (!directory.mkdirs()) {
                errorLog { "Failed to create payload directory: $directory" }
                return null
            }
        }
        val file = fileFor(key)
        // Never opened by readers; a crash mid-write leaves one orphan that the next write overwrites.
        // Callers serialize same-key writes.
        val tempFile = File(directory, file.name + TEMP_SUFFIX)
        return try {
            val checksum = FileOutputStream(tempFile).use { out ->
                encodeTo(out, payload).also { if (syncWrites) out.fd.sync() }
            }
            if (tempFile.renameTo(file)) checksum else null
        } catch (e: IOException) {
            errorLog(e) { "Failed to persist payload to disk." }
            null
        }
    }

    /**
     * Returns the payload, or `null` for a miss: no file, or bytes not matching [expectedChecksum],
     * the value [write] returned. Required, not optional: a failing payload decodes to U+FFFD garbage.
     */
    @Suppress("SwallowedException", "ReturnCount")
    fun read(key: String, expectedChecksum: Long): String? {
        return try {
            FileInputStream(fileFor(key)).use { input ->
                val sizeBytes = input.channel.size()
                // Not an integrity check: ByteArray would throw past the IOException catch below.
                if (sizeBytes > Int.MAX_VALUE) return null
                val bytes = ByteArray(sizeBytes.toInt())
                DataInputStream(input).readFully(bytes)
                val checksum = CRC32()
                checksum.update(bytes)
                if (checksum.value != expectedChecksum) return null
                // Lenient decoding, which the checksum above makes safe. CharsetDecoder.decode would
                // allocate a payload-sized char[] on top of `bytes`, OOMing on multi-MB responses.
                String(bytes, Charsets.UTF_8)
            }
        } catch (e: FileNotFoundException) {
            // No payload for this key: a plain cache miss, not an error.
            null
        } catch (e: IOException) {
            errorLog(e) { "Failed to read payload from disk." }
            null
        }
    }

    @Suppress("SwallowedException")
    fun checksumOf(payload: String): Long? {
        return try {
            encodeTo(DiscardingOutputStream, payload)
        } catch (exception: IOException) {
            null
        }
    }

    fun contains(key: String): Boolean = fileFor(key).exists()

    fun delete(key: String) {
        fileFor(key).delete()
    }

    fun clear() {
        // Renaming is a constant-time metadata op, safe on the main thread; the trashed directory is deleted by
        // the next write.
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

    private fun fileFor(key: String): File {
        return File(directory, Checksum.generate(key.toByteArray(), Checksum.Algorithm.SHA256).value)
    }

    /**
     * Streams [payload] to [rawOut] via a `char[]` chunk, returning the CRC32 of the bytes written:
     * `CharBuffer.wrap(payload)` has no backing array, which forces the encoder off its fast path and
     * measured a >60x slower store on ART. REPORT so an unencodable payload fails the write instead of
     * being silently altered.
     */
    private fun encodeTo(rawOut: OutputStream, payload: String): Long {
        val encoder = Charsets.UTF_8.newEncoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
        val checksum = CRC32()
        val out = CheckedOutputStream(rawOut, checksum)
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
        return checksum.value
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

    private object DiscardingOutputStream : OutputStream() {
        override fun write(byte: Int) = Unit
        override fun write(buffer: ByteArray, offset: Int, length: Int) = Unit
    }

    private companion object {
        const val VENDOR_DIRECTORY = "RevenueCat"
        const val TEMP_SUFFIX = ".tmp"
        const val TRASH_SUFFIX = ".trash"
        const val CHUNK_CHARS = 64 * 1024
        const val WRITE_BUFFER_BYTES = 256 * 1024
    }
}
