package com.revenuecat.purchases.models

import android.os.Parcelable
import com.revenuecat.purchases.utils.getDate
import kotlinx.parcelize.Parcelize
import org.json.JSONObject
import java.util.Date

@Parcelize
data class Transaction(
    val transactionIdentifier: String,
    val productIdentifier: String,
    val purchaseDate: Date
) : Parcelable {

    internal constructor(productId: String, jsonObject: JSONObject) : this(
        transactionIdentifier = jsonObject.getString("id"),
        productIdentifier = productId,
        purchaseDate = jsonObject.getDate("purchase_date")
    )
}
