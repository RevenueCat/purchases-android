package com.revenuecat.purchases.models

import android.os.Parcelable
import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.parceler.JSONObjectParceler
import kotlinx.android.parcel.Parcelize
import kotlinx.android.parcel.TypeParceler
import org.json.JSONObject

@Parcelize
@TypeParceler<JSONObject, JSONObjectParceler>()
// TODO: docs
data class PurchaseDetails(
    val orderId: String?,
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

    val originalJson: JSONObject,

    val presentedOfferingIdentifier: String?,

    val storeUserID: String?,

    val purchaseType: PurchaseType
) : Parcelable

enum class PurchaseType {
    GOOGLE_PURCHASE,
    GOOGLE_RESTORED_PURCHASE,
    AMAZON_PURCHASE
}
