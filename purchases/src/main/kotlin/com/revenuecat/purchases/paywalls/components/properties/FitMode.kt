package com.revenuecat.purchases.paywalls.components.properties

import com.revenuecat.purchases.InternalRevenueCatAPI
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@InternalRevenueCatAPI
@Serializable
internal enum class FitMode {
    @SerialName("fit")
    FIT,

    @SerialName("fill")
    FILL,
}
