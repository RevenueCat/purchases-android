package com.revenuecat.purchases

import com.revenuecat.purchases.util.Iso8601Utils

import org.json.JSONException
import org.json.JSONObject

import java.util.Date
import java.util.HashMap
import java.util.HashSet
/**
 * @property purchasedNonSubscriptionSkus Set of non-subscription, non-consumed skus
 * @property allExpirationDatesByProduct Map of skus to dates
 */
class PurchaserInfo private constructor(
    val purchasedNonSubscriptionSkus: Set<String>,
    val allExpirationDatesByProduct: Map<String, Date?>,
    val allExpirationDatesByEntitlement: Map<String, Date?>,
    internal val jsonObject: JSONObject
) {

    /**
     * @return Set of active subscription skus
     */
    fun getActiveSubscriptions(): Set<String> {
        return activeIdentifiers(allExpirationDatesByProduct)
    }

    /**
     * @return Set of purchased skus, active and inactive
     */
    fun getAllPurchasedSkus(): Set<String> {
        val appSKUs = HashSet(this.purchasedNonSubscriptionSkus)
        appSKUs.addAll(allExpirationDatesByProduct.keys)
        return appSKUs
    }

    /**
     * @return The latest expiration date of all purchased skus
     */
    fun getLatestExpirationDate(): Date? {
        return allExpirationDatesByProduct.values.sortedBy { it }.takeUnless { it.isEmpty() }?.last()
    }

    fun getActiveEntitlements(): Set<String> {
        return activeIdentifiers(allExpirationDatesByEntitlement)
    }

    /**
     * @param sku
     * @return Expiration date for given sku
     */
    fun getExpirationDateForSku(sku: String): Date? {
        return allExpirationDatesByProduct[sku]
    }

    /**
     * @param entitlement
     * @return Expiration date for given entitlement
     */
    fun getExpirationDateForEntitlement(entitlement: String): Date? {
        return allExpirationDatesByEntitlement[entitlement]
    }

    private fun activeIdentifiers(expirations: Map<String, Date?>): Set<String> {
        return expirations.filterValues { date -> date == null || date.after(Date()) }.keys
    }

    object Factory {

        /**
         * Parses expiration dates in a JSONObject
         * @param jsonObject JSONObject to deserialize
         * @throws [JSONException] If the json is invalid.
         */
        private fun parseExpirations(expirations: JSONObject): Map<String, Date?> {
            val expirationDates = HashMap<String, Date?>()

            val it = expirations.keys()
            while (it.hasNext()) {
                val key = it.next()

                val expirationObject = expirations.getJSONObject(key)

                if (expirationObject.isNull("expires_date")) {
                    expirationDates[key] = null
                } else {
                    val dateValue = expirationObject.getString("expires_date")
                    try {
                        val date = Iso8601Utils.parse(dateValue)
                        expirationDates[key] = date
                    } catch (e: RuntimeException) {
                        throw JSONException(e.message)
                    }
                }
            }

            return expirationDates
        }

        /**
         * Builds a PurchaserInfo
         * @param jsonObject JSONObject to deserialize
         * @throws [JSONException] If the json is invalid.
         */
        @Throws(JSONException::class)
        fun build(jsonObject: JSONObject): PurchaserInfo {
            val subscriber = jsonObject.getJSONObject("subscriber")

            val otherPurchases = subscriber.getJSONObject("other_purchases")
            val nonSubscriptionPurchases = HashSet<String>()

            val it = otherPurchases.keys()
            while (it.hasNext()) {
                val key = it.next()
                nonSubscriptionPurchases.add(key)
            }

            val subscriptions = subscriber.getJSONObject("subscriptions")
            val expirationDatesByProduct = parseExpirations(subscriptions)

            var entitlements = JSONObject()
            if (subscriber.has("entitlements")) {
                entitlements = subscriber.getJSONObject("entitlements")
            }

            val expirationDatesByEntitlement = parseExpirations(entitlements)

            return PurchaserInfo(
                nonSubscriptionPurchases,
                expirationDatesByProduct,
                expirationDatesByEntitlement,
                jsonObject
            )
        }
    }

}
