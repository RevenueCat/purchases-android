package com.revenuecat.purchases.models

import android.os.Parcelable
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.SkuDetails
import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.models.ComparableData
import com.revenuecat.purchases.models.StoreProduct
import com.revenuecat.purchases.parceler.JSONObjectParceler
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.RawValue
import kotlinx.parcelize.TypeParceler
import org.json.JSONObject

@Parcelize
@TypeParceler<JSONObject, JSONObjectParceler>()
data class PurchaseOption(
    val pricingPhases: List<PricingPhase>,
    val tags: List<String> = listOf(),
    val token: String // "offerToken" used for purchasing
) : Parcelable {
    val isBasePlan: Boolean
        get() = pricingPhases.size == 1

    val isFreeTrial: Boolean
        get() = pricingPhases[0].priceAmountMicros == 0L

    // idk if we can do something smarter here or if this is even valuable
     val isIntroPrice: Boolean
        get() = !isFreeTrial

}