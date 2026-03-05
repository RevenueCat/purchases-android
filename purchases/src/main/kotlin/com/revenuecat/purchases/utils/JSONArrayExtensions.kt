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
