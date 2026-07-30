package com.revenuecat.purchases.common.networking

import com.revenuecat.purchases.VerificationResult
import com.revenuecat.purchases.common.errorLog
import com.revenuecat.purchases.common.isSuccessful
import org.json.JSONException
import org.json.JSONObject
import java.util.Date

internal data class HTTPResult(
    val responseCode: Int,
    val payload: Payload,
    val origin: Origin,
    val requestDate: Date?,
    val verificationResult: VerificationResult,
    val isLoadShedderResponse: Boolean,
    val isFallbackURL: Boolean,
) {
    /**
     * Convenience constructor for textual (JSON) responses, which keeps the common call sites and the
     * ETag cache deserialization ergonomic. Wraps [payload] in [Payload.Text].
     */
    constructor(
        responseCode: Int,
        payload: String,
        origin: Origin,
        requestDate: Date?,
        verificationResult: VerificationResult,
        isLoadShedderResponse: Boolean,
        isFallbackURL: Boolean,
    ) : this(
        responseCode,
        Payload.Text(payload),
        origin,
        requestDate,
        verificationResult,
        isLoadShedderResponse,
        isFallbackURL,
    )

    /**
     * The response body, which is either textual (JSON, the common case) or raw [RCFormat] bytes (e.g.
     * the RC Container Format returned for `Accept: application/x-rc-format` requests).
     */
    sealed interface Payload {
        data class Text(val value: String) : Payload

        class RCFormat(val bytes: ByteArray) : Payload {
            override fun equals(other: Any?): Boolean =
                this === other || (other is RCFormat && bytes.contentEquals(other.bytes))

            override fun hashCode(): Int = bytes.contentHashCode()
        }

        /** The textual payload, or an empty string for a [Payload.RCFormat] body. */
        val text: String
            get() = when (this) {
                is Text -> value
                is RCFormat -> ""
            }
    }

    val payloadText: String = payload.text

    internal companion object {
        internal const val ETAG_HEADER_NAME = "X-RevenueCat-ETag"
        internal const val SIGNATURE_HEADER_NAME = "X-Signature"
        internal const val REQUEST_TIME_HEADER_NAME = "X-RevenueCat-Request-Time"
        internal const val LOAD_SHEDDER_HEADER_NAME = "x-revenuecat-fortress"
    }

    enum class Origin {
        BACKEND, CACHE
    }

    val body: JSONObject by lazy { parseBody() }

    private fun parseBody(): JSONObject {
        return payloadText
            .takeIf { it.isNotBlank() }
            ?.let {
                try {
                    JSONObject(it)
                } catch (e: JSONException) {
                    errorLog(throwable = e) { "Failed to parse payload as JSON: $it" }
                    null
                }
            }
            ?: JSONObject()
    }

    val backendErrorCode: Int? = if (!isSuccessful()) body.optInt("code").takeIf { it > 0 } else null
    val backendErrorMessage: String? = if (!isSuccessful()) {
        body.optString(
            "message",
        ).takeIf { it.isNotBlank() }
    } else {
        null
    }
}
