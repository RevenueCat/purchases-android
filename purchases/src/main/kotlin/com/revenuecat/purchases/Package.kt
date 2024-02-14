package com.revenuecat.purchases

import com.revenuecat.purchases.models.StoreProduct

/**
 * Contains information about the product available for the user to purchase. For more info see https://docs.revenuecat.com/docs/entitlements
 * @property identifier Unique identifier for this package. Can be one a predefined package type or a custom one.
 * @property packageType Package type for the product. Will be one of [PackageType].
 * @property product [StoreProduct] assigned to this package.
 * @property offering offering Id this package was returned from.
 * @property presentedOfferingContext [PresentedOfferingContext] from which this package was obtained.
 */
data class Package(
    val identifier: String,
    val packageType: PackageType,
    val product: StoreProduct,
    val presentedOfferingContext: PresentedOfferingContext,
) {
    @Deprecated(
        "Use constructor with presentedOfferingContext instead",
        ReplaceWith(
            "Package(identifier, packageType, product, offering, " +
                "PresentedOfferingContext(offeringIdentifier = offering))",
        ),
    )
    constructor(
        identifier: String,
        packageType: PackageType,
        product: StoreProduct,
        offering: String,
    ) : this(
        identifier,
        packageType,
        product,
        PresentedOfferingContext(offeringIdentifier = offering),
    )

    @Deprecated(
        "Use presentedOfferingContext.offeringIdentifier instead",
        ReplaceWith("presentedOfferingContext.offeringIdentifier"),
    )
    val offering: String
        get() = presentedOfferingContext.offeringIdentifier ?: ""
}

/**
 *  Enumeration of all possible Package types.
 */
enum class PackageType(val identifier: String?) {
    /**
     * A package that was defined with a custom identifier.
     */
    UNKNOWN(null),

    /**
     * A package that was defined with a custom identifier.
     */
    CUSTOM(null),

    /**
     * A package configured with the predefined lifetime identifier.
     */
    LIFETIME("\$rc_lifetime"),

    /**
     * A package configured with the predefined annual identifier.
     */
    ANNUAL("\$rc_annual"),

    /**
     * A package configured with the predefined six month identifier.
     */
    SIX_MONTH("\$rc_six_month"),

    /**
     * A package configured with the predefined three month identifier.
     */
    THREE_MONTH("\$rc_three_month"),

    /**
     * A package configured with the predefined two month identifier.
     */
    TWO_MONTH("\$rc_two_month"),

    /**
     * A package configured with the predefined monthly identifier.
     */
    MONTHLY("\$rc_monthly"),

    /**
     * A package configured with the predefined weekly identifier.
     */
    WEEKLY("\$rc_weekly"),
}
