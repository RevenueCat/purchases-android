package com.revenuecat.purchases.paywalls.components.properties

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal enum class FlexDistribution {
    @SerialName("start")
    START,

    @SerialName("end")
    END,

    @SerialName("center")
    CENTER,

    @SerialName("space_between")
    SPACE_BETWEEN,

    @SerialName("space_around")
    SPACE_AROUND,

    @SerialName("space_evenly")
    SPACE_EVENLY,
}
