package com.revenuecat.purchases.paywalls.components.properties

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.utils.serializers.EnumDeserializerWithDefault
import kotlinx.serialization.Serializable

@InternalRevenueCatAPI
@Serializable(with = HorizontalAlignmentDeserializer::class)
enum class HorizontalAlignment {
    // SerialNames are handled by the HorizontalAlignmentDeserializer.

    LEADING,
    CENTER,
    TRAILING,
}

@InternalRevenueCatAPI
@Serializable(with = VerticalAlignmentDeserializer::class)
enum class VerticalAlignment {
    // SerialNames are handled by the VerticalAlignmentDeserializer.

    TOP,
    CENTER,
    BOTTOM,
}

@InternalRevenueCatAPI
@Serializable(with = TwoDimensionalAlignmentDeserializer::class)
enum class TwoDimensionalAlignment {
    // SerialNames are handled by the TwoDimensionalAlignmentDeserializer.

    CENTER,
    LEADING,
    TRAILING,
    TOP,
    BOTTOM,
    TOP_LEADING,
    TOP_TRAILING,
    BOTTOM_LEADING,
    BOTTOM_TRAILING,
}

@OptIn(InternalRevenueCatAPI::class)
private object HorizontalAlignmentDeserializer : EnumDeserializerWithDefault<HorizontalAlignment>(
    defaultValue = HorizontalAlignment.LEADING,
)

@OptIn(InternalRevenueCatAPI::class)
private object VerticalAlignmentDeserializer : EnumDeserializerWithDefault<VerticalAlignment>(
    defaultValue = VerticalAlignment.TOP,
)

@OptIn(InternalRevenueCatAPI::class)
private object TwoDimensionalAlignmentDeserializer : EnumDeserializerWithDefault<TwoDimensionalAlignment>(
    defaultValue = TwoDimensionalAlignment.TOP,
)
