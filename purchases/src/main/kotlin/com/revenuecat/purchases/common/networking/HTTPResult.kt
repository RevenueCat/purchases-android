package com.revenuecat.purchases.common.networking

import com.revenuecat.purchases.VerificationResult
import com.revenuecat.purchases.common.errorLog
import com.revenuecat.purchases.common.isSuccessful
import org.json.JSONException
import org.json.JSONObject
import java.util.Date

private const val SERIALIZATION_NAME_RESPONSE_CODE = "responseCode"
private const val SERIALIZATION_NAME_PAYLOAD = "payload"
private const val SERIALIZATION_NAME_ORIGIN = "origin"
private const val SERIALIZATION_NAME_REQUEST_DATE = "requestDate"
private const val SERIALIZATION_NAME_VERIFICATION_RESULT = "verificationResult"
private const val SERIALIZATION_NAME_IS_LOAD_SHEDDER_RESPONSE = "isLoadShedderResponse"
private const val SERIALIZATION_NAME_IS_FALLBACK_URL = "isFallbackURL"

internal data class HTTPResult(
    val responseCode: Int,
    val payload: String,
    val origin: Origin,
    val requestDate: Date?,
    val verificationResult: VerificationResult,
    val isLoadShedderResponse: Boolean,
    val isFallbackURL: Boolean,
) {
    companion object {
        const val ETAG_HEADER_NAME = "X-RevenueCat-ETag"
        const val SIGNATURE_HEADER_NAME = "X-Signature"
        const val REQUEST_TIME_HEADER_NAME = "X-RevenueCat-Request-Time"
        const val LOAD_SHEDDER_HEADER_NAME = "x-revenuecat-fortress"

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
            val isLoadShedderResponse = if (jsonObject.has(SERIALIZATION_NAME_IS_LOAD_SHEDDER_RESPONSE)) {
                jsonObject.getBoolean(SERIALIZATION_NAME_IS_LOAD_SHEDDER_RESPONSE)
            } else {
                false
            }
            val isFallbackURL = if (jsonObject.has(SERIALIZATION_NAME_IS_FALLBACK_URL)) {
                jsonObject.getBoolean(SERIALIZATION_NAME_IS_FALLBACK_URL)
            } else {
                false
            }
            return HTTPResult(
                responseCode,
                payload,
                origin,
                requestDate,
                verificationResult,
                isLoadShedderResponse,
                isFallbackURL,
            )
        }
    }

    enum class Origin {
        BACKEND, CACHE
    }

    val body: JSONObject = payload
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

    val backendErrorCode: Int? = if (!isSuccessful()) body.optInt("code").takeIf { it > 0 } else null
    val backendErrorMessage: String? = if (!isSuccessful()) {
        body.optString(
            "message",
        ).takeIf { it.isNotBlank() }
    } else {
        null
    }

    fun serialize(): String {
        val jsonObject = JSONObject().apply {
            put(SERIALIZATION_NAME_RESPONSE_CODE, responseCode)
            put(SERIALIZATION_NAME_PAYLOAD, payload)
            put(SERIALIZATION_NAME_ORIGIN, origin.name)
            put(SERIALIZATION_NAME_REQUEST_DATE, requestDate?.time)
            put(SERIALIZATION_NAME_VERIFICATION_RESULT, verificationResult.name)
            put(SERIALIZATION_NAME_IS_LOAD_SHEDDER_RESPONSE, isLoadShedderResponse)
            put(SERIALIZATION_NAME_IS_FALLBACK_URL, isFallbackURL)
        }
        return jsonObject.toString()
    }
}
