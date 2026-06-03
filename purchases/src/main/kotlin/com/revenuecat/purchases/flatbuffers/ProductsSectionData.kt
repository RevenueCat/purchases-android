package com.revenuecat.purchases.flatbuffers

import com.revenuecat.purchases.flatbuffers.generated.ProductType as FbProductType

/**
 * Plain Kotlin domain models the rest of the SDK would consume.
 *
 * The generated FlatBuffers accessor types (under the `generated` package) never leak past
 * [FlatBuffersProductsSectionParser]; callers only ever see these classes. This mirrors how
 * existing factories (e.g. `CustomerInfoFactory`, `OfferingParser`) expose domain types rather
 * than the raw `JSONObject`.
 */
internal data class ProductsSectionData(
    val products: List<ProductData>,
    val fetchedAtMs: Long,
)

internal data class ProductData(
    val id: String,
    val title: String?,
    val priceMicros: Long,
    val currencyCode: String?,
    val type: ProductTypeData,
)

internal enum class ProductTypeData {
    SUBSCRIPTION,
    CONSUMABLE,
    NON_CONSUMABLE,
    UNKNOWN,
    ;

    internal companion object {
        /** Maps the generated `Byte` enum value onto the domain enum, defaulting to [UNKNOWN]. */
        fun fromFlatBuffer(value: Byte): ProductTypeData = when (value) {
            FbProductType.SUBSCRIPTION -> SUBSCRIPTION
            FbProductType.CONSUMABLE -> CONSUMABLE
            FbProductType.NON_CONSUMABLE -> NON_CONSUMABLE
            else -> UNKNOWN
        }
    }
}
