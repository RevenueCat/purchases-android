//  Purchases
//
//  Copyright © 2019 RevenueCat, Inc. All rights reserved.
//

package com.revenuecat.purchases

import android.os.Parcel
import android.util.Log
import com.android.billingclient.api.Purchase
import com.revenuecat.purchases.util.Iso8601Utils
import org.json.JSONException
import org.json.JSONObject
import java.util.Date

internal fun debugLog(message: String) {
    if (Purchases.debugLogsEnabled) {
        Log.d("[Purchases] - DEBUG", message)
    }
}

internal fun log(message: String) {
    Log.w("[Purchases] - INFO", message)
}

internal fun errorLog(message: String) {
    if (Purchases.debugLogsEnabled) {
        Log.e("[Purchases] - ERROR", message)
    }
}

internal fun Purchase.toHumanReadableDescription() =
    "${this.sku} ${this.orderId} ${this.purchaseToken}"

internal fun Parcel.readStringDateMap(): Map<String, Date?> {
    return readInt().let { size ->
        (0 until size).map {
            readString() to readLong().let { date ->
                if (date == -1L) null else Date(date)
            }
        }.toMap()
    }
}

internal fun Parcel.writeStringDateMap(mapStringDate: Map<String, Date?>) {
    writeInt(mapStringDate.size)
    mapStringDate.forEach { (entry, date) ->
        writeString(entry)
        writeLong(date?.time ?: -1)
    }
}

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

        if (expirationObject.isNull(jsonKey)) {
            expirationDates[key] = null
        } else {
            val dateValue = expirationObject.getString(jsonKey)
            try {
                val date = Iso8601Utils.parse(dateValue)
                expirationDates[key] = date
            } catch (e: RuntimeException) {
                throw JSONException(e.message)
            }
        }
    }

    return expirationDates
}
