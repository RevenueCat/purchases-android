package com.revenuecat.purchases.utils

import org.json.JSONArray
import org.json.JSONObject

internal fun <T> JSONArray.toList(): List<T>? {
    val list = mutableListOf<T>()

    for (i in 0 until this.length()) {
        val value = when (val rawValue = this.get(i)) {
            is JSONObject -> rawValue.toMap<T>(deep = true)
            is JSONArray -> rawValue.toList<T>()
            else -> rawValue
        }

        @Suppress("UNCHECKED_CAST")
        list.add(value as T)
    }

    return list
}

internal fun <T> List<T?>.replaceJsonNullWithKotlinNull(): List<T?> {
    @Suppress("UNCHECKED_CAST")
    return this.map { item ->
        when (item) {
            is Map<*, *> ->
                @Suppress("UNCHECKED_CAST")
                (item as Map<T, T?>).replaceJsonNullWithKotlinNull()
            is List<*> ->
                @Suppress("UNCHECKED_CAST")
                (item as List<T?>).replaceJsonNullWithKotlinNull()
            JSONObject.NULL -> null
            else -> item
        }
    } as List<T?>
}

/**
 * Returns a lazy [Sequence] over the elements of this [JSONArray].
 *
 * This removes the need for manual index-based iteration
 * (`for (i in 0 until length())`) and enables functional operators
 * such as `map`, `flatMap`, and `filter`.
 *
 * Elements are returned as nullable `Any?` because a JSONArray
 * may contain mixed or null values.
 */
internal fun JSONArray.asSequence(): Sequence<Any?> = sequence {
    for (index in 0 until length()) {
        yield(opt(index))
    }
}

/**
 * Returns a [Sequence] of [JSONObject] elements contained in this array.
 *
 * Non-JSONObject elements are ignored.
 *
 * This provides a safe and expressive way to traverse
 * JSON arrays that are expected to contain objects,
 * avoiding unsafe casts and manual type checks.
 */
internal fun JSONArray.objects(): Sequence<JSONObject> =
    asSequence().filterIsInstance<JSONObject>()
