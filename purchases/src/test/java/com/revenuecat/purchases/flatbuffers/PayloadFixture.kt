@file:OptIn(ExperimentalUnsignedTypes::class)

package com.revenuecat.purchases.flatbuffers

import android.util.Base64
import com.google.flatbuffers.FlatBufferBuilder
import com.revenuecat.purchases.flatbuffers.generated.Blob
import com.revenuecat.purchases.flatbuffers.generated.Payload

/**
 * Demonstrates the symmetric "backend side": how a producer would build the FlatBuffer with
 * [FlatBufferBuilder] and base64-encode it for embedding in a JSON field. Used by the parser
 * roundtrip test in place of a real backend.
 */
internal fun encodePayloadBase64(
    config: ByteArray,
    blobs: List<Pair<ByteArray, ByteArray>>,
): String {
    val builder = FlatBufferBuilder(INITIAL_BUFFER_BYTES)

    // Each blob's key/value vectors must be created before the Blob table that references them,
    // and the blobs vector before the Payload that references it.
    val blobOffsets = blobs.map { (key, value) ->
        val keyOffset = Blob.createKeyVector(builder, key.toUByteArray())
        val valueOffset = Blob.createValueVector(builder, value.toUByteArray())
        Blob.createBlob(builder, keyOffset, valueOffset)
    }.toIntArray()

    val blobsVector = Payload.createBlobsVector(builder, blobOffsets)
    val configVector = Payload.createConfigVector(builder, config.toUByteArray())
    val root = Payload.createPayload(builder, configVector, blobsVector)
    builder.finish(root)

    return Base64.encodeToString(builder.sizedByteArray(), Base64.NO_WRAP)
}

private const val INITIAL_BUFFER_BYTES = 256
