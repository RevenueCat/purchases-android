package com.revenuecat.purchases.common.networking

import org.json.JSONObject

private const val SERIALIZATION_NAME_RESPONSE_CODE = "responseCode"
private const val SERIALIZATION_NAME_PAYLOAD = "payload"

data class HTTPResult(
    val responseCode: Int,
    val payload: String
) {
    val body: JSONObject = payload.takeIf { it.isNotBlank() }?.let { JSONObject(it) } ?: JSONObject()

    fun serialize(): String {
        val jsonObject = JSONObject().apply {
            put(SERIALIZATION_NAME_RESPONSE_CODE, responseCode)
            put(SERIALIZATION_NAME_PAYLOAD, payload)
        }
        return jsonObject.toString()
    }

    companion object {
        fun deserialize(serialized: String): HTTPResult {
            val jsonObject = JSONObject(serialized)
            val responseCode = jsonObject.getInt(SERIALIZATION_NAME_RESPONSE_CODE)
            val payload = jsonObject.getString(SERIALIZATION_NAME_PAYLOAD)
            return HTTPResult(responseCode, payload)
        }
    }
}
