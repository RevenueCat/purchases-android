package com.revenuecat.purchases.paywalls.components.properties

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal enum class FitMode {
    @SerialName("fit")
    FIT,

    @SerialName("fill")
    FILL,
}
