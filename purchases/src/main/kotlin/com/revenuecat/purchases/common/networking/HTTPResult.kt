package com.revenuecat.purchases.common.networking

import com.revenuecat.purchases.VerificationResult
import org.json.JSONObject
import java.util.Date

private const val SERIALIZATION_NAME_RESPONSE_CODE = "responseCode"
private const val SERIALIZATION_NAME_PAYLOAD = "payload"
private const val SERIALIZATION_NAME_ORIGIN = "origin"
private const val SERIALIZATION_NAME_REQUEST_DATE = "requestDate"
private const val SERIALIZATION_NAME_VERIFICATION_RESULT = "verificationResult"

internal data class HTTPResult(
    val responseCode: Int,
    val payload: String,
    val origin: Origin,
    val requestDate: Date?,
    val verificationResult: VerificationResult,
) {
    companion object {
        const val ETAG_HEADER_NAME = "X-RevenueCat-ETag"
        const val SIGNATURE_HEADER_NAME = "X-Signature"
        const val REQUEST_TIME_HEADER_NAME = "X-RevenueCat-Request-Time"

        fun deserialize(serialized: String): HTTPResult {
            val jsonObject = JSONObject(serialized)
            val responseCode = jsonObject.getInt(SERIALIZATION_NAME_RESPONSE_CODE)
            val payload = jsonObject.getString(SERIALIZATION_NAME_PAYLOAD)
            val origin: Origin = if (jsonObject.has(SERIALIZATION_NAME_ORIGIN)) {
                Origin.valueOf(jsonObject.getString(SERIALIZATION_NAME_ORIGIN))
            } else {
                Origin.CACHE
            }
            val requestDate: Date? = if (jsonObject.has(SERIALIZATION_NAME_REQUEST_DATE)) {
                Date(jsonObject.getLong(SERIALIZATION_NAME_REQUEST_DATE))
            } else {
                null
            }
            val verificationResult: VerificationResult = if (jsonObject.has(SERIALIZATION_NAME_VERIFICATION_RESULT)) {
                VerificationResult.valueOf(jsonObject.getString(SERIALIZATION_NAME_VERIFICATION_RESULT))
            } else {
                VerificationResult.NOT_REQUESTED
            }
            return HTTPResult(responseCode, payload, origin, requestDate, verificationResult)
        }
    }

    enum class Origin {
        BACKEND, CACHE
    }

    val body: JSONObject = payload.takeIf { it.isNotBlank() }?.let { JSONObject(it) } ?: JSONObject()

    fun serialize(): String {
        val jsonObject = JSONObject().apply {
            put(SERIALIZATION_NAME_RESPONSE_CODE, responseCode)
            put(SERIALIZATION_NAME_PAYLOAD, payload)
            put(SERIALIZATION_NAME_ORIGIN, origin.name)
            put(SERIALIZATION_NAME_REQUEST_DATE, requestDate?.time)
            put(SERIALIZATION_NAME_VERIFICATION_RESULT, verificationResult.name)
        }
        return jsonObject.toString()
    }
}
