//  Purchases
//
//  Copyright © 2019 RevenueCat, Inc. All rights reserved.
//

package com.revenuecat.purchases

import android.content.Context
import android.os.Build
import android.os.Parcel
import android.os.Parcelable
import android.util.Base64
import android.util.Log
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchaseHistoryRecord
import com.android.billingclient.api.SkuDetails
import com.revenuecat.purchases.util.Iso8601Utils
import kotlinx.android.parcel.Parceler
import org.json.JSONException
import org.json.JSONObject
import java.security.MessageDigest
import java.util.Date
import java.util.Locale

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

internal fun <T : Parcelable> Parcel.readStringParcelableMap(loader: ClassLoader?): Map<String, T> {
    return readInt().let { size ->
        (0 until size).map {
            readString() to readParcelable<T>(loader)
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

internal fun Parcel.writeStringParcelableMap(mapStringParcelable: Map<String, Parcelable>) {
    writeInt(mapStringParcelable.size)
    mapStringParcelable.forEach { (entry, parcelable) ->
        writeString(entry)
        writeParcelable(parcelable, 0)
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

internal fun Context.getLocale(): Locale? =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        resources.configuration.locales.get(0)
    } else {
        @Suppress("DEPRECATION")
        resources.configuration.locale
    }

internal fun Locale.toBCP47(): String {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        return toLanguageTag()
    }

    // we will use a dash as per BCP 47
    val separator = '-'
    var language = language
    var region = country
    var variant = variant

    // special case for Norwegian Nynorsk since "NY" cannot be a variant as per BCP 47
    // this goes before the string matching since "NY" wont pass the variant checks
    if (language == "no" && region == "NO" && variant == "NY") {
        language = "nn"
        region = "NO"
        variant = ""
    }

    if (language.isEmpty() || !language.matches("\\p{Alpha}{2,8}".toRegex())) {
        language = "und" // Follow the Locale#toLanguageTag() implementation
        // which says to return "und" for Undetermined
    } else if (language == "iw") {
        language = "he" // correct deprecated "Hebrew"
    } else if (language == "in") {
        language = "id" // correct deprecated "Indonesian"
    } else if (language == "ji") {
        language = "yi" // correct deprecated "Yiddish"
    }

    // ensure valid country code, if not well formed, it's omitted
    if (!region.matches("\\p{Alpha}{2}|\\p{Digit}{3}".toRegex())) {
        region = ""
    }

    // variant subtags that begin with a letter must be at least 5 characters long
    if (!variant.matches("\\p{Alnum}{5,8}|\\p{Digit}\\p{Alnum}{3}".toRegex())) {
        variant = ""
    }

    val bcp47Tag = StringBuilder(language)
    if (region.isNotEmpty()) {
        bcp47Tag.append(separator).append(region)
    }
    if (variant.isNotEmpty()) {
        bcp47Tag.append(separator).append(variant)
    }

    return bcp47Tag.toString()
}

data class PlatformInfo(
    val flavor: String,
    val version: String?
)

internal fun String.sha1() =
    MessageDigest.getInstance("SHA-1")
        .digest(this.toByteArray()).let {
            String(Base64.encode(it, Base64.NO_WRAP))
        }

internal fun JSONObject.getNullableString(name: String): String? = this.getString(name).takeUnless { this.isNull(name) }

/** @suppress */
internal object SkuDetailsParceler : Parceler<SkuDetails> {

    override fun create(parcel: Parcel): SkuDetails {
        return SkuDetails(parcel.readString())
    }

    override fun SkuDetails.write(parcel: Parcel, flags: Int) {
        val field = SkuDetails::class.java.getDeclaredField("mOriginalJson")
        field.isAccessible = true
        val value = field.get(this) as String
        parcel.writeString(value)
    }
}

/** @suppress */
internal object JSONObjectParceler : Parceler<JSONObject> {

    override fun create(parcel: Parcel): JSONObject {
        return JSONObject(parcel.readString())
    }

    override fun JSONObject.write(parcel: Parcel, flags: Int) {
        val field = JSONObject::class.java.getDeclaredField("jsonObject")
        field.isAccessible = true
        val value = field.get(this).toString()
        parcel.writeString(value)
    }
}

internal fun BillingResult.toHumanReadableDescription() =
    "DebugMessage: $debugMessage. ErrorCode: ${responseCode.getBillingResponseCodeName()}."

internal fun PurchaseHistoryRecord.toHumanReadableDescription() =
    "${this.sku} ${this.purchaseTime} ${this.purchaseToken}"

val SkuDetails.priceAmount: Double
    get() = this.priceAmountMicros.div(1000000.0)

internal val Context.versionName: String?
    get() = this.packageManager.getPackageInfo(this.packageName, 0).versionName
