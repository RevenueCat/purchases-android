package com.revenuecat.purchases.common.networking

import org.json.JSONObject

private const val SERIALIZATION_NAME_RESPONSE_CODE = "responseCode"
private const val SERIALIZATION_NAME_PAYLOAD = "payload"
private const val SERIALIZATION_NAME_ORIGIN = "origin"

enum class ResultOrigin {
    BACKEND, CACHE
}

data class HTTPResult(
    val responseCode: Int,
    val payload: String,
    val origin: ResultOrigin
) {
    val body: JSONObject = payload.takeIf { it.isNotBlank() }?.let { JSONObject(it) } ?: JSONObject()

    fun serialize(): String {
        val jsonObject = JSONObject().apply {
            put(SERIALIZATION_NAME_RESPONSE_CODE, responseCode)
            put(SERIALIZATION_NAME_PAYLOAD, payload)
            put(SERIALIZATION_NAME_ORIGIN, origin.name)
        }
        return jsonObject.toString()
    }

    companion object {
        fun deserialize(serialized: String): HTTPResult {
            val jsonObject = JSONObject(serialized)
            val responseCode = jsonObject.getInt(SERIALIZATION_NAME_RESPONSE_CODE)
            val payload = jsonObject.getString(SERIALIZATION_NAME_PAYLOAD)
            val origin: ResultOrigin = if (jsonObject.has(SERIALIZATION_NAME_ORIGIN)) {
                ResultOrigin.valueOf(jsonObject.getString(SERIALIZATION_NAME_ORIGIN))
            } else {
                ResultOrigin.CACHE
            }
            return HTTPResult(responseCode, payload, origin)
        }
    }
}
