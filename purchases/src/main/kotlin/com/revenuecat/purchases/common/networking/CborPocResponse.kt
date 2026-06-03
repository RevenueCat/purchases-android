package com.revenuecat.purchases.common.networking

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.common.CborTools
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromByteArray

/**
 * Proof-of-concept network response model used to evaluate CBOR decoding.
 *
 * It intentionally contains a representative mix of field types (strings, longs, booleans, a list,
 * a nested object and optional fields) so that a JSON-vs-CBOR comparison is meaningful. It does not
 * correspond to any real backend endpoint and is independent from the production response models.
 */
@Serializable
internal data class CborPocResponse(
    @SerialName("request_date_ms") val requestDateMs: Long,
    @SerialName("subscriber_id") val subscriberId: String,
    @SerialName("is_sandbox") val isSandbox: Boolean,
    val entitlements: List<CborPocEntitlement>,
    val metadata: CborPocMetadata? = null,
) {
    internal companion object {
        @OptIn(ExperimentalSerializationApi::class)
        fun fromBytes(bytes: ByteArray): CborPocResponse {
            return CborTools.cbor.decodeFromByteArray(bytes)
        }

        /**
         * Decodes from an [HTTPResult] whose body was received as CBOR bytes.
         * Returns null if the result carries no CBOR payload.
         */
        @OptIn(InternalRevenueCatAPI::class)
        fun fromHTTPResult(result: HTTPResult): CborPocResponse? {
            return result.payloadBytes?.let { fromBytes(it) }
        }
    }
}

@Serializable
internal data class CborPocEntitlement(
    val identifier: String,
    @SerialName("product_identifier") val productIdentifier: String,
    @SerialName("expires_date_ms") val expiresDateMs: Long? = null,
    @SerialName("is_active") val isActive: Boolean,
)

@Serializable
internal data class CborPocMetadata(
    @SerialName("schema_version") val schemaVersion: Int,
    val tags: List<String> = emptyList(),
)
