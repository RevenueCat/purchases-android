package com.revenuecat.purchases.common.networking

import org.json.JSONArray
import org.json.JSONObject

/**
 * A class to convert a Map<String, Any?> into a JSONObject.
 * This was created to workaround a bug in Android 4 , where a List<String> would be incorrectly converted into
 * a single string instead of a JSONArray of strings. (i.e.: "[\"value1\", \"value2\"]" instead of "[value1, value2]")
 * This class handles nested maps, lists, and other JSON-compatible types.
 */
internal class MapConverter {

    /**
     * Converts the given [inputMap] into a JSONObject.
     *
     * @param inputMap The input map to convert.
     * @return A JSONObject representing the input map.
     */
    internal fun convertToJSON(inputMap: Map<String, Any?>): JSONObject {
        val mapWithoutInnerMaps = inputMap.mapValues { (_, value) ->
            when (value) {
                is List<*> -> {
                    if (value.all { it is String }) {
                        JSONObject(mapOf("temp_key" to JSONArray(value))).getJSONArray("temp_key")
                    } else {
                        value
                    }
                }
                else -> value.tryCast<Map<String, Any?>>(ifSuccess = { convertToJSON(this) })
            }
        }
        return createJSONObject(mapWithoutInnerMaps)
    }

    internal fun createJSONObject(inputMap: Map<String, Any?>): JSONObject {
        return JSONObject(inputMap)
    }

    /** To avoid Java type erasure, we use a Kotlin inline function with a reified parameter
     * so that we can check the type on runtime.
     *
     * Doing something like:
     * if (value is Map<*, *>) (value as Map<String, Any?>).convert()
     *
     * Would give an unchecked cast warning due to Java type erasure
     */
    private inline fun <reified T> Any?.tryCast(
        ifSuccess: T.() -> Any?,
    ): Any? {
        return if (this is T) {
            this.ifSuccess()
        } else {
            this
        }
    }
}
