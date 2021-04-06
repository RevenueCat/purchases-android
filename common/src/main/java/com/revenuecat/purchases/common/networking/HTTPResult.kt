package com.revenuecat.purchases.common.networking

import org.json.JSONObject

data class HTTPResult(
    val responseCode: Int,
    val payload: String
) {
    val body: JSONObject = payload.takeIf { it.isNotBlank() }?.let { JSONObject(it) } ?: JSONObject()

    fun serialize(): String {
        val jsonObject = JSONObject()
        jsonObject.put("responseCode", responseCode)
        jsonObject.put("payload", payload)
        return jsonObject.toString()
    }

    companion object {
        fun deserialize(serialized: String): HTTPResult {
            val jsonObject = JSONObject(serialized)
            val responseCode = jsonObject.getInt("responseCode")
            val payload = jsonObject.getString("payload")
            return HTTPResult(responseCode, payload)
        }
    }
}
