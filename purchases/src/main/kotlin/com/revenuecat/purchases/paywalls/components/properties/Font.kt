package com.revenuecat.purchases.paywalls.components.properties

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.utils.serializers.EnumDeserializerWithDefault
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@InternalRevenueCatAPI
@Serializable(with = FontWeightDeserializer::class)
enum class FontWeight {
    // SerialNames are handled by the FontWeightDeserializer.

    EXTRA_LIGHT,
    THIN,
    LIGHT,
    REGULAR,
    MEDIUM,
    SEMI_BOLD,
    BOLD,
    EXTRA_BOLD,
    BLACK,
}

@Deprecated(
    "Font sizes are just integers now. Remove after 2025-03-01 when we are sure no more paywalls are using this.",
)
@Suppress("MagicNumber")
@InternalRevenueCatAPI
@Serializable
enum class FontSize {
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

@OptIn(InternalRevenueCatAPI::class)
private object FontWeightDeserializer : EnumDeserializerWithDefault<FontWeight>(
    defaultValue = FontWeight.REGULAR,
    typeForValue = { value ->
        when (value) {
            FontWeight.EXTRA_LIGHT -> "extra_light"
            FontWeight.THIN -> "thin"
            FontWeight.LIGHT -> "light"
            FontWeight.REGULAR -> "regular"
            FontWeight.MEDIUM -> "medium"
            FontWeight.SEMI_BOLD -> "semibold"
            FontWeight.BOLD -> "bold"
            FontWeight.EXTRA_BOLD -> "extra_bold"
            FontWeight.BLACK -> "black"
        }
    },
)
