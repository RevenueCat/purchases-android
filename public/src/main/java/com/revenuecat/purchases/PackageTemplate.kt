package com.revenuecat.purchases

import android.os.Parcelable
import com.revenuecat.purchases.models.StoreProduct
import kotlinx.parcelize.Parcelize

/**
 * Contains information about the product available for the user to purchase. For more info see https://docs.revenuecat.com/docs/entitlements
 * @property identifier Unique identifier for this package. Can be one a predefined package type or a custom one.
 * @property packageType Package type for the product. Will be one of [PackageType].
 * @property product [StoreProduct] assigned to this package.
 * @property offering offeringID this package was returned from.
 * @property group_identifier group this package belongs to (usually corresponds with the subscriptionId).
 * @property offering offeringID this package was returned from.
 */
@Parcelize
data class PackageTemplate(
    val identifier: String,
    val packageType: PackageType,
    val offering: String,
    val product_identifier: String,
    val group_identifier: String?,
    val duration: String?
) : Parcelable {
    fun makePackage(product: StoreProduct): Package {
        return Package(identifier, packageType, product, offering)
    }
}