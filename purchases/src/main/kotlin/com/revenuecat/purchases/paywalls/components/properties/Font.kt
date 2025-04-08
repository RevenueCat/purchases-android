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
    valuesByType = mapOf(
        "extra_light" to FontWeight.EXTRA_LIGHT,
        "thin" to FontWeight.THIN,
        "light" to FontWeight.LIGHT,
        "regular" to FontWeight.REGULAR,
        "medium" to FontWeight.MEDIUM,
        "semibold" to FontWeight.SEMI_BOLD,
        "bold" to FontWeight.BOLD,
        "extra_bold" to FontWeight.EXTRA_BOLD,
        "black" to FontWeight.BLACK,
    ),
    defaultValue = FontWeight.REGULAR,
)
