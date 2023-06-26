package com.revenuecat.purchases.utils

import org.json.JSONArray
import org.json.JSONObject
import java.util.Date

fun JSONObject.getDate(jsonKey: String): Date = Iso8601Utils.parse(getString(jsonKey))

fun JSONObject.optDate(jsonKey: String): Date? = takeUnless { this.isNull(jsonKey) }?.getDate(jsonKey)

fun JSONObject.getNullableString(name: String): String? = takeUnless { this.isNull(name) }?.getString(name)

fun JSONObject.optNullableString(name: String): String? = takeIf { this.has(name) }?.getNullableString(name)

fun <T> JSONObject.toMap(deep: Boolean = false): Map<String, T>? {
    return this.keys()?.asSequence()?.map { jsonKey ->
        if (deep) {
            val value = when (val rawValue = this[jsonKey]) {
                is JSONObject -> rawValue.toMap<T>()
                is JSONArray -> rawValue.toList<T>()
                else -> rawValue
            }

            @Suppress("UNCHECKED_CAST")
            jsonKey to value as T
        } else {
            @Suppress("UNCHECKED_CAST")
            jsonKey to this[jsonKey] as T
        }
    }?.toMap()
}
