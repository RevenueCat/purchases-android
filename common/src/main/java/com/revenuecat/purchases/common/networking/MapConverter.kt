package com.revenuecat.purchases.common.networking

import org.json.JSONArray
import org.json.JSONObject

class MapConverter {

    fun convertToJSON(inputMap: Map<String, Any?>?): JSONObject {
        return inputMap.convert()
    }

    private fun Map<String, Any?>?.convert(): JSONObject {
        if (this == null) {
            return JSONObject()
        }
        val mapWithoutInnerMaps = mapValues { (_, value) ->
            value.tryCast<Map<String, Any?>>(ifSuccess = { convert() })
            when (value) {
                is List<*> -> {
                    if (value.all { it is String }) {
                        JSONObject(mapOf("temp_key" to JSONArray(value))).getJSONArray("temp_key")
                    } else {
                        value
                    }
                }
                else -> value.tryCast<Map<String, Any?>>(ifSuccess = { convert() })
            }
        }
        return JSONObject(mapWithoutInnerMaps)
    }

    // To avoid Java type erasure, we use a Kotlin inline function with a reified parameter
    // so that we can check the type on runtime.
    //
    // Doing something like:
    // if (value is Map<*, *>) (value as Map<String, Any?>).convert()
    //
    // Would give an unchecked cast warning due to Java type erasure
    private inline fun <reified T> Any?.tryCast(
        ifSuccess: T.() -> Any?
    ): Any? {
        return if (this is T) {
            this.ifSuccess()
        } else {
            this
        }
    }
}
