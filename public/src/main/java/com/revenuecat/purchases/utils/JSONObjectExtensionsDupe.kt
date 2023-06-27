package com.revenuecat.purchases.utils

import org.json.JSONArray
import org.json.JSONObject
import java.util.Date

/**
 * TODO this is a dupe of JSONObjectExtensions in common module, but it's used in the public module so we can't remove
 * it yet, nor make it public here because it would be accessible to everyone
 */
internal fun JSONObject.getDate(jsonKey: String): Date = Iso8601UtilsDupe.parse(getString(jsonKey))

internal fun JSONObject.optDate(jsonKey: String): Date? = takeUnless { this.isNull(jsonKey) }?.getDate(jsonKey)

internal fun JSONObject.getNullableString(name: String): String? = takeUnless { this.isNull(name) }?.getString(name)

internal fun JSONObject.optNullableString(name: String): String? = takeIf { this.has(name) }?.getNullableString(name)

internal fun <T> JSONObject.toMap(deep: Boolean = false): Map<String, T>? {
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
