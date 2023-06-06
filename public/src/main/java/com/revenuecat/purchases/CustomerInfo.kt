//  Purchases
//
//  Copyright © 2019 RevenueCat, Inc. All rights reserved.
//

package com.revenuecat.purchases

import android.net.Uri
import android.os.Parcelable
import com.revenuecat.purchases.models.RawDataContainer
import com.revenuecat.purchases.models.Transaction
import com.revenuecat.purchases.utils.DateHelper
import com.revenuecat.purchases.utils.JSONObjectParceler
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.TypeParceler
import org.json.JSONObject
import java.util.Date

/**
 * Class containing all information regarding the customer
 * @property entitlements Entitlements attached to this customer info
 * @property allExpirationDatesByProduct Map of productIds to expiration dates
 * For Google subscriptions, productIds are subscriptionId:basePlanId
 * For Amazon subscriptions, productsIds are termSkus
 * @property allPurchaseDatesByProduct Map of productIds to purchase dates
 * For Google subscriptions, productIds are subscriptionId:basePlanId
 * For Google and Amazon INAPPs, productsIds are simply productId.
 * For Amazon subscriptions, productsIds are termSkus
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
data class CustomerInfo constructor(
    val entitlements: EntitlementInfos,
    val allExpirationDatesByProduct: Map<String, Date?>,
    val allPurchaseDatesByProduct: Map<String, Date?>,
    val requestDate: Date,
    val schemaVersion: Int,
    val firstSeen: Date,
    val originalAppUserId: String,
    val managementURL: Uri?,
    val originalPurchaseDate: Date?,
    private val jsonObject: JSONObject,
) : Parcelable, RawDataContainer<JSONObject> {

    /**
     * @return Set of active subscription productIds
     *
     * For Google subscriptions, productIds will be subscriptionId:basePlanId
     * For Amazon subscriptions, productIds will be termSku
     */
    @IgnoredOnParcel
    val activeSubscriptions: Set<String> by lazy {
        activeIdentifiers(allExpirationDatesByProduct)
    }

    /**
     * @return Set of purchased skus, active and inactive
     */
    @IgnoredOnParcel
    @Deprecated(
        "Use allPurchasedProductIds instead",
        ReplaceWith("allPurchasedProductIds"),
    )
    val allPurchasedSkus: Set<String> by lazy {
        this.nonSubscriptionTransactions.map { it.productIdentifier }.toSet() + allExpirationDatesByProduct.keys
    }

    /**
     * @return Set of purchased productIds, active and inactive
     *
     * For Google subscriptions, productIds are subscriptionId:basePlanId
     * For Google and Amazon INAPPs, productsIds are simply productId.
     * For Amazon subscriptions, productsIds are termSkus
     */
    @IgnoredOnParcel
    val allPurchasedProductIds: Set<String> by lazy {
        this.nonSubscriptionTransactions.map { it.productIdentifier }.toSet() + allExpirationDatesByProduct.keys
    }

    /**
     * @return The latest expiration date of all purchased productIds
     */
    @IgnoredOnParcel
    val latestExpirationDate: Date? by lazy {
        allExpirationDatesByProduct.values.sortedBy { it }.takeUnless { it.isEmpty() }?.last()
    }

    /**
     * @return List of all non subscription transactions. Use this to fetch the history of
     * non-subscription purchases
     */
    @IgnoredOnParcel
    val nonSubscriptionTransactions: List<Transaction> by lazy {
        val nonSubscriptionTransactionList = mutableListOf<Transaction>()
        val nonSubscriptions = subscriberJSONObject.getJSONObject("non_subscriptions")
        nonSubscriptions.keys().forEach { productId ->
            val arrayOfNonSubscriptions = nonSubscriptions.getJSONArray(productId)
            for (i in 0 until arrayOfNonSubscriptions.length()) {
                val transactionJSONObject = arrayOfNonSubscriptions.getJSONObject(i)
                val transaction =
                    Transaction(productId, transactionJSONObject)
                nonSubscriptionTransactionList.add(transaction)
            }
        }
        nonSubscriptionTransactionList.sortedBy { it.purchaseDate }
    }

    /**
     * Get the expiration date for a given sku
     * @param sku Sku for which to retrieve expiration date
     * @return Expiration date for given sku
     */
    @Deprecated(
        "Use getExpirationDateForProductId instead",
        ReplaceWith("getExpirationDateForProductId"),
    )
    fun getExpirationDateForSku(sku: String): Date? {
        return allExpirationDatesByProduct[sku]
    }

    /**
     * Get the expiration date for a given productId
     * @param productId productId for which to retrieve expiration date
     * For Google subscriptions, productIds are subscriptionId:basePlanId
     *
     * For Amazon subscriptions, productsIds are termSkus
     * @return Expiration date for given productId
     */
    fun getExpirationDateForProductId(productId: String): Date? {
        return allExpirationDatesByProduct[productId]
    }

    /**
     * Get the latest purchase or renewal date for given sku
     * @param sku Sku for which to retrieve expiration date
     * @return Purchase date for given sku
     */
    @Deprecated(
        "Use getPurchaseDateForProductId instead",
        ReplaceWith("getPurchaseDateForProductId"),
    )
    fun getPurchaseDateForSku(sku: String): Date? {
        return allPurchaseDatesByProduct[sku]
    }

    /**
     * Get the latest purchase or renewal date for given productId
     * @param productId productId for which to retrieve expiration date
     * For Google subscriptions, productIds are subscriptionId:basePlanId
     * For Google and Amazon INAPPs, productsIds are simply productId.
     * For Amazon subscriptions, productsIds are termSkus
     * @return Purchase date for given productId
     */
    fun getPurchaseDateForProductId(productId: String): Date? {
        return allPurchaseDatesByProduct[productId]
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

    @IgnoredOnParcel
    override val rawData: JSONObject
        get() = jsonObject

    private fun activeIdentifiers(expirations: Map<String, Date?>): Set<String> {
        return expirations.filterValues {
                expirationDate ->
            DateHelper.isDateActive(expirationDate, requestDate).isActive
        }.keys
    }

    @IgnoredOnParcel
    private val subscriberJSONObject = jsonObject.getJSONObject("subscriber")

    /**
     * @hide
     */
    override fun toString() =
        "<CustomerInfo\n " +
            "latestExpirationDate: $latestExpirationDate\n" +
            "activeSubscriptions:  ${activeSubscriptions.map {
                it to mapOf("expiresDate" to getExpirationDateForProductId(it))
            }.toMap()},\n" +
            "activeEntitlements: ${entitlements.active.map { it.toString() }},\n" +
            "entitlements: ${entitlements.all.map { it.toString() }},\n" +
            "nonSubscriptionTransactions: $nonSubscriptionTransactions,\n" +
            "requestDate: $requestDate\n>"

    override fun equals(other: Any?) = other is CustomerInfo && ComparableData(this) == ComparableData(other)
    override fun hashCode() = ComparableData(this).hashCode()
}

/**
 * Contains fields to be used for equality, which ignores requestDate and jsonObject.
 * requestDate is excluded so that two CustomerInfo objects that are otherwise identical
 * won't be considered different if they were refreshed at a different point in time
 * jsonObject is excluded because we're already using the parsed fields for comparisons.
 */
private data class ComparableData(
    val entitlements: EntitlementInfos,
    val allExpirationDatesByProduct: Map<String, Date?>,
    val allPurchaseDatesByProduct: Map<String, Date?>,
    val schemaVersion: Int,
    val firstSeen: Date,
    val originalAppUserId: String,
    val originalPurchaseDate: Date?,
) {
    constructor(
        customerInfo: CustomerInfo,
    ) : this(
        entitlements = customerInfo.entitlements,
        allExpirationDatesByProduct = customerInfo.allExpirationDatesByProduct,
        allPurchaseDatesByProduct = customerInfo.allPurchaseDatesByProduct,
        schemaVersion = customerInfo.schemaVersion,
        firstSeen = customerInfo.firstSeen,
        originalAppUserId = customerInfo.originalAppUserId,
        originalPurchaseDate = customerInfo.originalPurchaseDate,
    )
}
