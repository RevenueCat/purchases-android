//  Purchases
//
//  Copyright © 2019 RevenueCat, Inc. All rights reserved.
//

package com.revenuecat.purchases

import android.net.Uri
import android.os.Parcelable
import com.revenuecat.purchases.models.Transaction
import kotlinx.android.parcel.Parcelize
import kotlinx.android.parcel.TypeParceler
import org.json.JSONObject
import java.util.Date

/**
 * Class containing all information regarding the purchaser
 * @property entitlements Entitlements attached to this purchaser info
 * @property purchasedNonSubscriptionSkus Set of non-subscription, non-consumed skus
 * @property allExpirationDatesByProduct Map of skus to expiration dates
 * @property allPurchaseDatesByProduct Map of skus to purchase dates
 * @property nonSubscriptionTransactions Map of Transactions per product ID
 * @property requestDate Date when this info was requested
 * @property firstSeen The date this user was first seen in RevenueCat.
 * @property originalAppUserId The original App User Id recorded for this user.
 * @property managementURL URL to manage the active subscription of the user. If this user has an active iOS
 * subscription, this will point to the App Store, if the user has an active Play Store subscription
 * it will point there. If there are no active subscriptions it will be null.
 * If there are multiple for different platforms, it will point to the Play Store
 * @property originalPurchaseDate the purchase date for the version of the application when the user bought the app.
 * Use this for grandfathering users when migrating to subscriptions. This can be null, see -Purchases.restorePurchases
 */
@Parcelize
@TypeParceler<JSONObject, JSONObjectParceler>()
data class PurchaserInfo internal constructor(
    val entitlements: EntitlementInfos,
    @Deprecated(
        "Use nonSubscriptionTransactions instead",
        ReplaceWith("nonSubscriptionTransactions.keys")
    ) val purchasedNonSubscriptionSkus: Set<String>,
    val allExpirationDatesByProduct: Map<String, Date?>,
    val allPurchaseDatesByProduct: Map<String, Date?>,
    val nonSubscriptionTransactions: Map<String, List<Transaction>>,
    val requestDate: Date,
    internal val jsonObject: JSONObject,
    internal val schemaVersion: Int,
    val firstSeen: Date,
    val originalAppUserId: String,
    val managementURL: Uri?,
    val originalPurchaseDate: Date?
) : Parcelable {

    /**
     * @return Set of active subscription skus
     */
    val activeSubscriptions: Set<String>
        get() = activeIdentifiers(allExpirationDatesByProduct)

    /**
     * @return Set of purchased skus, active and inactive
     */
    val allPurchasedSkus: Set<String>
        get() = this.nonSubscriptionTransactions.keys + allExpirationDatesByProduct.keys

    /**
     * @return The latest expiration date of all purchased skus
     */
    val latestExpirationDate: Date?
        get() = allExpirationDatesByProduct.values.sortedBy { it }.takeUnless { it.isEmpty() }?.last()

    /**
     * Get the expiration date for a given sku
     * @param sku Sku for which to retrieve expiration date
     * @return Expiration date for given sku
     */
    fun getExpirationDateForSku(sku: String): Date? {
        return allExpirationDatesByProduct[sku]
    }

    /**
     * Get the latest purchase or renewal date for given sku
     * @param sku Sku for which to retrieve expiration date
     * @return Purchase date for given sku
     */
    fun getPurchaseDateForSku(sku: String): Date? {
        return allPurchaseDatesByProduct[sku]
    }

    /**
     * Get the expiration date for a given entitlement identifier.
     * @param entitlement Entitlement for which to return expiration date
     * @return Expiration date for a given entitlement
     */
    fun getExpirationDateForEntitlement(entitlement: String): Date? {
        return entitlements.all[entitlement]?.expirationDate
    }

    /**
     * Get the latest purchase or renewal date for a given entitlement identifier.
     * @param entitlement Entitlement for which to return purchase date
     * @return Purchase date for given entitlement
     */
    fun getPurchaseDateForEntitlement(entitlement: String): Date? {
        return entitlements.all[entitlement]?.latestPurchaseDate
    }

    private fun activeIdentifiers(expirations: Map<String, Date?>): Set<String> {
        return expirations.filterValues { date -> date == null || date.after(requestDate) }.keys
    }

    /**
     * @hide
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PurchaserInfo

        if (nonSubscriptionTransactions != other.nonSubscriptionTransactions) return false
        if (allExpirationDatesByProduct != other.allExpirationDatesByProduct) return false
        if (allPurchaseDatesByProduct != other.allPurchaseDatesByProduct) return false
        if (entitlements != other.entitlements) return false
        if (schemaVersion != other.schemaVersion) return false
        if (firstSeen != other.firstSeen) return false
        if (originalAppUserId != other.originalAppUserId) return false

        return true
    }

    /**
     * @hide
     */
    override fun toString() =
        "<PurchaserInfo\n " +
                "latestExpirationDate: $latestExpirationDate\n" +
                "activeSubscriptions:  ${activeSubscriptions.map {
                    it to mapOf("expiresDate" to getExpirationDateForSku(it))
                }.toMap()},\n" +
                "activeEntitlements: ${entitlements.active.map { it.toString() }},\n" +
                "entitlements: ${entitlements.all.map { it.toString() }},\n" +
                "nonSubscriptionTransactions: $nonSubscriptionTransactions,\n" +
                "requestDate: $requestDate\n>"

    override fun hashCode(): Int {
        var result = entitlements.hashCode()
        result = 31 * result + nonSubscriptionTransactions.hashCode()
        result = 31 * result + allExpirationDatesByProduct.hashCode()
        result = 31 * result + allPurchaseDatesByProduct.hashCode()
        result = 31 * result + requestDate.hashCode()
        result = 31 * result + jsonObject.hashCode()
        result = 31 * result + schemaVersion
        result = 31 * result + firstSeen.hashCode()
        result = 31 * result + originalAppUserId.hashCode()
        return result
    }

    /**
    * @hide
    */
    companion object {

        internal const val SCHEMA_VERSION = 3
    }
}
