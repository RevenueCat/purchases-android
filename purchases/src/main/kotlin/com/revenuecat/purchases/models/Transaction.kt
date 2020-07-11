package com.revenuecat.purchases.models

import android.os.Parcelable
import com.revenuecat.purchases.getDate
import kotlinx.android.parcel.Parcelize
import org.json.JSONObject
import java.util.Date

@Parcelize
data class Transaction(
    val transactionId: String,
    val productId: String,
    val purchaseDate: Date
) : Parcelable {
    constructor(productId: String, jsonObject: JSONObject) : this(
        transactionId = jsonObject.getString("id"),
        productId = productId,
        purchaseDate = jsonObject.getDate("purchase_date")
    )
}
