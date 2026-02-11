package com.revenuecat.purchases

import com.revenuecat.purchases.models.StoreProduct
import dev.drewhamilton.poko.Poko
import java.net.URL

/**
 * Contains information about the product available for the user to purchase. For more info see https://docs.revenuecat.com/docs/entitlements
 * @property identifier Unique identifier for this package. Can be one a predefined package type or a custom one.
 * @property packageType Package type for the product. Will be one of [PackageType].
 * @property product [StoreProduct] assigned to this package.
 * @property offering offering Id this package was returned from.
 * @property presentedOfferingContext [PresentedOfferingContext] from which this package was obtained.
 * @property webCheckoutURL If the Offering has an associated Web Purchase Link with a product in this package,
 * this will be the URL for it linking directly to purchase this package.
 */
@Poko
public class Package @JvmOverloads constructor(
    public val identifier: String,
    public val packageType: PackageType,
    public val product: StoreProduct,
    public val presentedOfferingContext: PresentedOfferingContext,
    public val webCheckoutURL: URL? = null,
) {
    @Deprecated(
        "Use constructor with presentedOfferingContext instead",
        ReplaceWith(
            "Package(identifier, packageType, product, " +
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
        webCheckoutURL = null,
    )

    @Deprecated(
        "Use presentedOfferingContext.offeringIdentifier instead",
        ReplaceWith("presentedOfferingContext.offeringIdentifier"),
    )
    public val offering: String
        get() = presentedOfferingContext.offeringIdentifier ?: ""

    internal fun copy(presentedOfferingContext: PresentedOfferingContext): Package {
        return Package(
            identifier = this.identifier,
            packageType = this.packageType,
            product = this.product.copyWithPresentedOfferingContext(presentedOfferingContext),
            presentedOfferingContext = presentedOfferingContext,
            webCheckoutURL = this.webCheckoutURL,
        )
    }
}

/**
 *  Enumeration of all possible Package types.
 */
public enum class PackageType(val identifier: String?) {
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
