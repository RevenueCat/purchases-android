package com.revenuecat.purchases.paywalls.components.properties

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal enum class HorizontalAlignment {
    @SerialName("leading")
    LEADING,

    @SerialName("center")
    CENTER,

    @SerialName("trailing")
    TRAILING,
}

@Serializable
internal enum class VerticalAlignment {
    @SerialName("top")
    TOP,

    @SerialName("center")
    CENTER,

    @SerialName("bottom")
    BOTTOM,
}

@Serializable
internal enum class TwoDimensionalAlignment {
    @SerialName("center")
    CENTER,

    @SerialName("leading")
    LEADING,

    @SerialName("trailing")
    TRAILING,

    @SerialName("top")
    TOP,

    @SerialName("bottom")
    BOTTOM,

    @SerialName("top_leading")
    TOP_LEADING,

    @SerialName("top_trailing")
    TOP_TRAILING,

    @SerialName("bottom_leading")
    BOTTOM_LEADING,

    @SerialName("bottom_trailing")
    BOTTOM_TRAILING,
}
