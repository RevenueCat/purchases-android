package com.revenuecat.purchases.utils

import org.json.JSONArray
import org.json.JSONObject

fun <T> JSONArray.toList(): List<T>? {
    val list = mutableListOf<T>()

    for (i in 0 until this.length()) {
        val value = when (val rawValue = this.get(i)) {
            is JSONObject -> rawValue.toMap<T>()
            is JSONArray -> rawValue.toList<T>()
            else -> rawValue
        }

        @Suppress("UNCHECKED_CAST")
        list.add(value as T)
    }

    return list
}
