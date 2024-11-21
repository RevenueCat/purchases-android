package com.revenuecat.purchases.paywalls.components.properties

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal enum class FontWeight {
    @SerialName("extra_light")
    EXTRA_LIGHT,

    @SerialName("thin")
    THIN,

    @SerialName("light")
    LIGHT,

    @SerialName("regular")
    REGULAR,

    @SerialName("medium")
    MEDIUM,

    @SerialName("semi_bold")
    SEMI_BOLD,

    @SerialName("bold")
    BOLD,

    @SerialName("extra_bold")
    EXTRA_BOLD,

    @SerialName("black")
    BLACK,
}

@Serializable
internal enum class FontSize {
    @SerialName("heading_xxl")
    HEADING_XXL,

    @SerialName("heading_xl")
    HEADING_XL,

    @SerialName("heading_l")
    HEADING_L,

    @SerialName("heading_m")
    HEADING_M,

    @SerialName("heading_s")
    HEADING_S,

    @SerialName("heading_xs")
    HEADING_XS,

    @SerialName("body_xl")
    BODY_XL,

    @SerialName("body_l")
    BODY_L,

    @SerialName("body_m")
    BODY_M,

    @SerialName("body_s")
    BODY_S,
}
