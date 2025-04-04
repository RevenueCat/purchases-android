package com.revenuecat.purchases.models

import android.os.Parcelable
import androidx.annotation.VisibleForTesting
import com.revenuecat.purchases.Store
import com.revenuecat.purchases.common.responses.PriceResponse
import com.revenuecat.purchases.utils.getDate
import com.revenuecat.purchases.utils.optDate
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.json.Json
import org.json.JSONObject
import java.util.Date
import java.util.Locale

@Parcelize
data class Transaction(
    val transactionIdentifier: String,
    @Deprecated(
        "Use transactionIdentifier instead",
        ReplaceWith("transactionIdentifier"),
    )
    val revenuecatId: String,
    val productIdentifier: String,
    @Deprecated(
        "Use productIdentifier instead",
        ReplaceWith("productIdentifier"),
    )
    val productId: String,
    val purchaseDate: Date,
    val storeTransactionId: String?,
    val store: Store,
    val displayName: String?,
    val isSandbox: Boolean = false,
    val originalPurchaseDate: Date?,
    val price: Price?,
) : Parcelable {

    internal companion object {
        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        internal val json = Json {
            ignoreUnknownKeys = true
        }
    }

    internal constructor(
        productId: String,
        jsonObject: JSONObject,
        locale: Locale = Locale.getDefault(),
    ) : this(
        transactionIdentifier = jsonObject.getString("id"),
        revenuecatId = jsonObject.getString("id"),
        productIdentifier = productId,
        productId = productId,
        purchaseDate = jsonObject.getDate("purchase_date"),
        storeTransactionId = jsonObject.optString("store_transaction_id").takeIf {
            it.isNotBlank()
        },
        store = jsonObject.getString("store").let { Store.fromString(it) },
        displayName = jsonObject.optString("display_name").takeIf {
            it.isNotBlank()
        },
        isSandbox = jsonObject.optBoolean("sandbox", false),
        originalPurchaseDate = jsonObject.optDate("original_purchase_date"),
        // Using the PriceResponse class to parse the price JSON object to make it easier to migrate
        // to Kotlin serialization in the future
        price = jsonObject.optJSONObject("price")?.toString()?.let {
            json.decodeFromString<PriceResponse>(it).toPrice(locale)
        },
    )
}
