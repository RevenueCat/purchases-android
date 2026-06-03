package com.revenuecat.purchases.common

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.cbor.Cbor

/**
 * Proof-of-concept CBOR decoder, mirroring [com.revenuecat.purchases.JsonTools] for JSON.
 *
 * CBOR (RFC 8949) is a compact binary encoding. We are evaluating whether decoding network
 * responses from CBOR bytes is more performant than parsing JSON text.
 */
internal object CborTools {

    @OptIn(ExperimentalSerializationApi::class)
    val cbor = Cbor {
        ignoreUnknownKeys = true
    }
}
