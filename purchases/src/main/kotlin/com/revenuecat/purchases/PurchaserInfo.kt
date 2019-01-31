//  Purchases
//
//  Copyright © 2019 RevenueCat, Inc. All rights reserved.
//

package com.revenuecat.purchases

import android.os.Parcel
import android.os.Parcelable
import org.json.JSONObject
import java.util.Date

/**
 * Class containing all information regarding the purchaser
 * @property purchasedNonSubscriptionSkus Set of non-subscription, non-consumed skus
 * @property allExpirationDatesByProduct Map of skus to expiration dates
 * @property allPurchaseDatesByProduct Map of skus to purchase dates
 * @property allExpirationDatesByEntitlement Map of entitlement ids to expiration dates
 * @property allPurchaseDatesByEntitlement Map of entitlement ids to purchase dates
 * @property requestDate Date when this info was requested
 */
class PurchaserInfo internal constructor(
    val purchasedNonSubscriptionSkus: Set<String>,
    val allExpirationDatesByProduct: Map<String, Date?>,
    val allPurchaseDatesByProduct: Map<String, Date?>,
    val allExpirationDatesByEntitlement: Map<String, Date?>,
    val allPurchaseDatesByEntitlement: Map<String, Date?>,
    val requestDate: Date?,
    internal val jsonObject: JSONObject
) : Parcelable {
    /**
     * @hide
     */
    constructor(parcel: Parcel): this(
        parcel.readInt().let { size -> (0 until size).map { parcel.readString() }.toSet() },
        parcel.readStringDateMap(),
        parcel.readStringDateMap(),
        parcel.readStringDateMap(),
        parcel.readStringDateMap(),
        parcel.readLong().let { date -> if (date == -1L) null else Date(date) },
        JSONObject(parcel.readString())
    )

    /**
     * @return Set of active subscription skus
     */
    val activeSubscriptions: Set<String>
        get() = activeIdentifiers(allExpirationDatesByProduct)

    /**
     * @return Set of purchased skus, active and inactive
     */
    val allPurchasedSkus: Set<String>
        get() = this.purchasedNonSubscriptionSkus + allExpirationDatesByProduct.keys

    /**
     * @return The latest expiration date of all purchased skus
     */
    val latestExpirationDate: Date?
        get() = allExpirationDatesByProduct.values.sortedBy { it }.takeUnless { it.isEmpty() }?.last()

    /**
     * The identifiers of all the active entitlements
     */
    val activeEntitlements: Set<String>
        get() = activeIdentifiers(allExpirationDatesByEntitlement)

    /**
     * Get the expiration date for a given sku
     * @param sku Sku for which to retrieve expiration date
     * @return Expiration date for given sku
     */
    fun getExpirationDateForSku(sku: String): Date? {
        return allExpirationDatesByProduct[sku]
    }

    /**
     * Get the purchase date for given sku
     * @param sku Sku for which to retrieve expiration date
     * @return Purchase date for given sku
     */
    fun getPurchaseDateForSku(sku: String): Date? {
        return allPurchaseDatesByProduct[sku]
    }

    /**
     * Get the expiration date for a given entitlement
     * @param entitlement Entitlement for which to return expiration date
     * @return Expiration date for a given entitlement
     */
    fun getExpirationDateForEntitlement(entitlement: String): Date? {
        return allExpirationDatesByEntitlement[entitlement]
    }

    /**
     * Get the purchase date for a given entitlement
     * @param entitlement Entitlement for which to return purchase date
     * @return Purchase date for given entitlement
     */
    fun getPurchaseDateForEntitlement(entitlement: String): Date? {
        return allPurchaseDatesByEntitlement[entitlement]
    }

    private fun activeIdentifiers(expirations: Map<String, Date?>): Set<String> {
        return expirations.filterValues { date -> date == null || date.after(requestDate ?: Date()) }.keys
    }

    /**
     * @hide
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PurchaserInfo

        if (allExpirationDatesByProduct != other.allExpirationDatesByProduct) return false
        if (allPurchaseDatesByProduct != other.allPurchaseDatesByProduct) return false
        if (allExpirationDatesByEntitlement != other.allExpirationDatesByEntitlement) return false
        if (allPurchaseDatesByEntitlement != other.allPurchaseDatesByEntitlement) return false
        if (purchasedNonSubscriptionSkus != other.purchasedNonSubscriptionSkus) return false
        if (activeEntitlements != other.activeEntitlements) return false

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
                "activeEntitlements: ${activeEntitlements.map {
                    it to mapOf("expiresDate" to getExpirationDateForEntitlement(it))
                }.toMap()},\n" +
                "nonConsumablePurchases: $purchasedNonSubscriptionSkus,\n" +
                "requestDate: $requestDate\n>"

    /**
     * @hide
     */
    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(purchasedNonSubscriptionSkus.size)
        purchasedNonSubscriptionSkus.forEach { entry -> parcel.writeString(entry) }
        parcel.writeStringDateMap(allExpirationDatesByProduct)
        parcel.writeStringDateMap(allPurchaseDatesByProduct)
        parcel.writeStringDateMap(allExpirationDatesByEntitlement)
        parcel.writeStringDateMap(allPurchaseDatesByEntitlement)
        parcel.writeLong(requestDate?.time ?: -1)
        parcel.writeString(jsonObject.toString())
    }

    /**
     * @hide
     */
    override fun describeContents(): Int {
        return 0
    }

    /**
     * @hide
     */
    override fun hashCode(): Int {
        var result = purchasedNonSubscriptionSkus.hashCode()
        result = 31 * result + allExpirationDatesByProduct.hashCode()
        result = 31 * result + allPurchaseDatesByProduct.hashCode()
        result = 31 * result + allExpirationDatesByEntitlement.hashCode()
        result = 31 * result + allPurchaseDatesByEntitlement.hashCode()
        result = 31 * result + (requestDate?.hashCode() ?: 0)
        result = 31 * result + jsonObject.hashCode()
        return result
    }

    /**
     * @hide
     */
    companion object CREATOR : Parcelable.Creator<PurchaserInfo> {
        /**
         * @hide
         */
        override fun createFromParcel(parcel: Parcel): PurchaserInfo {
            return PurchaserInfo(parcel)
        }
        /**
         * @hide
         */
        override fun newArray(size: Int): Array<PurchaserInfo?> {
            return arrayOfNulls(size)
        }
    }
}
