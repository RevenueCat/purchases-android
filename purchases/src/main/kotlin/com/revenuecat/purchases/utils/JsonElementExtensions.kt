package com.revenuecat.purchases.utils

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

fun JsonElement.asMap(): Map<String, Any?>? {
    if (this is JsonObject) {
        return jsonObject.entries.associate {
            it.key to it.value.extractedContent
        }
    }
    return null
}

private val JsonElement.extractedContent: Any?
    get() {
        return when (this) {
            is JsonPrimitive -> {
                with(jsonPrimitive) {
                    if (isString) {
                        content
                    } else {
                        booleanOrNull
                            ?: intOrNull
                            ?: longOrNull
                            ?: floatOrNull
                            ?: doubleOrNull
                            ?: contentOrNull
                    }
                }
            }
            is JsonArray -> {
                jsonArray.map {
                    it.extractedContent
                }
            }
            is JsonObject -> {
                jsonObject.entries.associate {
                    it.key to it.value.extractedContent
                }
            }
            else -> null
        }
    }
