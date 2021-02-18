package com.revenuecat.purchases.utils

import org.json.JSONException
import org.json.JSONObject
import java.util.Date

/**
 * Parses expiration dates in a JSONObject
 * @throws [JSONException] If the json is invalid.
 */
fun JSONObject.parseExpirations(): Map<String, Date?> {
    return parseDates("expires_date")
}

/**
 * Parses purchase dates in a JSONObject
 * @throws [JSONException] If the json is invalid.
 */
fun JSONObject.parsePurchaseDates(): Map<String, Date?> {
    return parseDates("purchase_date")
}

/**
 * Parses dates that match a JSON key in a JSONObject
 * @param jsonKey Key of the dates to deserialize from the JSONObject
 * @throws [JSONException] If the json is invalid.
 */
fun JSONObject.parseDates(jsonKey: String): HashMap<String, Date?> {
    val expirationDates = HashMap<String, Date?>()

    val it = keys()
    while (it.hasNext()) {
        val key = it.next()

        val expirationObject = getJSONObject(key)
        expirationDates[key] = expirationObject.optDate(jsonKey)
    }

    return expirationDates
}

fun JSONObject.getDate(jsonKey: String): Date = Iso8601Utils.parse(getString(jsonKey))

fun JSONObject.optDate(jsonKey: String): Date? = takeUnless { this.isNull(jsonKey) }?.getDate(jsonKey)

fun JSONObject.getNullableString(name: String): String? = takeUnless { this.isNull(name) }?.getString(name)

fun JSONObject.optNullableString(name: String): String? = takeIf { this.has(name) }?.getNullableString(name)

fun <T> JSONObject.toMap(): Map<String, T>? {
    return this.keys()?.asSequence()?.map { jsonKey ->
        @Suppress("UNCHECKED_CAST")
        jsonKey to this[jsonKey] as T
    }?.toMap()
}
