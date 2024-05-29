package com.revenuecat.purchases.models

import android.os.Parcelable
import com.revenuecat.purchases.utils.getDate
import kotlinx.parcelize.Parcelize
import org.json.JSONObject
import java.util.Date

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
    var shouldConsume: Boolean
) : Parcelable {

    internal constructor(productId: String, jsonObject: JSONObject) : this(
        transactionIdentifier = jsonObject.getString("id"),
        revenuecatId = jsonObject.getString("id"),
        productIdentifier = productId,
        productId = productId,
        purchaseDate = jsonObject.getDate("purchase_date"),
        shouldConsume = jsonObject.optBoolean("play_iap_should_consume", true),
    )
}
