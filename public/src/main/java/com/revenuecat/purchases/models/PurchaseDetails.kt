package com.revenuecat.purchases.models

import android.os.Parcelable
import com.android.billingclient.api.Purchase
import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.parceler.JSONObjectParceler
import kotlinx.android.parcel.Parcelize
import kotlinx.android.parcel.TypeParceler
import org.json.JSONObject

@Parcelize
@TypeParceler<JSONObject, JSONObjectParceler>()
// TODO: docs
data class PurchaseDetails(
    val orderId: String,
    val sku: String,
    val type: ProductType,
    val purchaseTime: Long,
    val purchaseToken: String,
    val purchaseState: RevenueCatPurchaseState,

    /*
     * Null for restored purchases
     */
    val isAutoRenewing: Boolean?,

    /*
     * Null for Amazon
     */
    val signature: String?,

    val originalJson: JSONObject
) : Parcelable

// TODO: should this be nullable or just throw
val PurchaseDetails.originalGooglePurchase: Purchase?
    get() = this.signature?.let { signature ->
        Purchase(this.originalJson.toString(), signature)
    }
