package com.revenuecat.purchases.flatbuffers

/**
 * Plain Kotlin domain models the rest of the SDK would consume.
 *
 * The generated FlatBuffers accessor types (under the `generated` package) never leak past
 * [FlatBuffersPayloadParser]; callers only ever see these classes. This mirrors how existing
 * factories (e.g. `CustomerInfoFactory`, `OfferingParser`) expose domain types rather than the
 * raw `JSONObject`.
 */
internal data class PayloadData(
    val config: ByteArray,
    val blobs: List<BlobEntry>,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PayloadData) return false
        return config.contentEquals(other.config) && blobs == other.blobs
    }

    override fun hashCode(): Int = 31 * config.contentHashCode() + blobs.hashCode()
}

/**
 * One entry of the schema's `blobs` "map". Modeled as a key/value pair because FlatBuffers has no
 * native map type and `ByteArray` is unsound as a [Map] key (identity-based equality).
 */
internal data class BlobEntry(
    val key: ByteArray,
    val value: ByteArray,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is BlobEntry) return false
        return key.contentEquals(other.key) && value.contentEquals(other.value)
    }

    override fun hashCode(): Int = 31 * key.contentHashCode() + value.contentHashCode()
}
