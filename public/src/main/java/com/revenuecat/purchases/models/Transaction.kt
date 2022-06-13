package com.revenuecat.purchases.models

import android.os.Parcelable
import com.revenuecat.purchases.utils.getDate
import kotlinx.parcelize.Parcelize
import org.json.JSONObject
import java.util.Date

@Parcelize
data class Transaction(
    val revenuecatId: String,
    val productId: String,
    val purchaseDate: Date
) : Parcelable {

    internal constructor(productId: String, jsonObject: JSONObject) : this(
        revenuecatId = jsonObject.getString("id"),
        productId = productId,
        purchaseDate = jsonObject.getDate("purchase_date")
    )
}
