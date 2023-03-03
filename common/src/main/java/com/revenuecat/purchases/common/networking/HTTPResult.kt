package com.revenuecat.purchases.common.networking

import org.json.JSONObject

private const val SERIALIZATION_NAME_RESPONSE_CODE = "responseCode"
private const val SERIALIZATION_NAME_PAYLOAD = "payload"
private const val SERIALIZATION_NAME_ORIGIN = "origin"
private const val SERIALIZATION_NAME_VERIFICATION_STATUS = "verificationStatus"

data class HTTPResult(
    val responseCode: Int,
    val payload: String,
    val origin: Origin,
    val verificationStatus: VerificationStatus
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
            val verificationStatus: VerificationStatus = if (jsonObject.has(SERIALIZATION_NAME_VERIFICATION_STATUS)) {
                VerificationStatus.valueOf(jsonObject.getString(SERIALIZATION_NAME_VERIFICATION_STATUS))
            } else {
                VerificationStatus.NOT_VERIFIED
            }
            return HTTPResult(responseCode, payload, origin, verificationStatus)
        }
    }

    enum class Origin {
        BACKEND, CACHE
    }

    enum class VerificationStatus {
        NOT_VERIFIED, SUCCESS, ERROR
    }

    val body: JSONObject = payload.takeIf { it.isNotBlank() }?.let { JSONObject(it) } ?: JSONObject()

    fun serialize(): String {
        val jsonObject = JSONObject().apply {
            put(SERIALIZATION_NAME_RESPONSE_CODE, responseCode)
            put(SERIALIZATION_NAME_PAYLOAD, payload)
            put(SERIALIZATION_NAME_ORIGIN, origin.name)
            put(SERIALIZATION_NAME_VERIFICATION_STATUS, verificationStatus.name)
        }
        return jsonObject.toString()
    }
}
