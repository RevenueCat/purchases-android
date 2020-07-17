package com.revenuecat.purchases.utils

import com.revenuecat.purchases.util.Iso8601Utils
import org.json.JSONException
import org.json.JSONObject
import java.util.Date

/**
 * Parses expiration dates in a JSONObject
 * @throws [JSONException] If the json is invalid.
 */
internal fun JSONObject.parseExpirations(): Map<String, Date?> {
    return parseDates("expires_date")
}

/**
 * Parses purchase dates in a JSONObject
 * @throws [JSONException] If the json is invalid.
 */
internal fun JSONObject.parsePurchaseDates(): Map<String, Date?> {
    return parseDates("purchase_date")
}

/**
 * Parses dates that match a JSON key in a JSONObject
 * @param jsonKey Key of the dates to deserialize from the JSONObject
 * @throws [JSONException] If the json is invalid.
 */
internal fun JSONObject.parseDates(jsonKey: String): HashMap<String, Date?> {
    val expirationDates = HashMap<String, Date?>()

    val it = keys()
    while (it.hasNext()) {
        val key = it.next()

        val expirationObject = getJSONObject(key)
        expirationDates[key] = expirationObject.optDate(jsonKey)
    }

    return expirationDates
}

internal fun JSONObject.getDate(jsonKey: String): Date = Iso8601Utils.parse(getString(jsonKey))

internal fun JSONObject.optDate(jsonKey: String): Date? = takeUnless { this.isNull(jsonKey) }?.getDate(jsonKey)

internal fun JSONObject.getNullableString(name: String): String? = this.getString(name).takeUnless { this.isNull(name) }

internal fun JSONObject.optNullableString(name: String): String? = this.optString(name).takeUnless { this.isNull(name) }
