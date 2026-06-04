package com.revenuecat.purchases.flatbuffers

import android.util.Base64
import com.revenuecat.purchases.common.errorLog
import com.revenuecat.purchases.flatbuffers.generated.Payload
import org.json.JSONObject
import java.nio.ByteBuffer

/**
 * Proof of concept: parses a FlatBuffers-encoded "section" delivered inside a regular JSON
 * backend response.
 *
 * The backend embeds the binary FlatBuffer as a base64 string in a dedicated JSON field
 * ([PAYLOAD_FIELD]). We read that one field from the already-parsed `HTTPResult.body`,
 * base64-decode it, and read the buffer with zero-copy generated accessors.
 *
 * Error handling intentionally matches `OfferingParser`: any failure is logged and yields
 * `null` so a malformed section can never crash response parsing.
 */
internal object FlatBuffersPayloadParser {

    private const val PAYLOAD_FIELD = "payload_fb"

    @Suppress("TooGenericExceptionCaught")
    fun parse(body: JSONObject): PayloadData? {
        val base64 = body.optString(PAYLOAD_FIELD).takeIf { it.isNotBlank() }
            ?: return null

        return try {
            val bytes = Base64.decode(base64, Base64.NO_WRAP)
            val payload = Payload.getRootAsPayload(ByteBuffer.wrap(bytes))

            val blobs = (0 until payload.blobsLength).mapNotNull { index ->
                payload.blobs(index)?.let { blob ->
                    BlobEntry(
                        key = readByteVector(blob.keyLength) { blob.keyAsByteBuffer },
                        value = readByteVector(blob.valueLength) { blob.valueAsByteBuffer },
                    )
                }
            }

            PayloadData(
                config = readByteVector(payload.configLength) { payload.configAsByteBuffer },
                blobs = blobs,
            )
        } catch (e: Throwable) {
            errorLog(e) { "Error parsing FlatBuffers payload" }
            null
        }
    }

    /**
     * Copies a `[ubyte]` vector into a [ByteArray]. The length guard avoids touching the
     * `*AsByteBuffer` accessor when the (optional) vector is absent.
     */
    private inline fun readByteVector(length: Int, byteBuffer: () -> ByteBuffer): ByteArray {
        if (length == 0) return ByteArray(0)
        val buffer = byteBuffer()
        return ByteArray(buffer.remaining()).also { buffer.get(it) }
    }
}
