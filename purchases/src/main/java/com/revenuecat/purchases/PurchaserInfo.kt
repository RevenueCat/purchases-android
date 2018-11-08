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
 * @property allExpirationDatesByEntitlement Map of entitlement ids to dates
 * @property requestDate Date when this info was requested
 */
class PurchaserInfo private constructor(
    val purchasedNonSubscriptionSkus: Set<String>,
    val allExpirationDatesByProduct: Map<String, Date?>,
    val allExpirationDatesByEntitlement: Map<String, Date?>,
    val requestDate: Date?,
    val aliasToken: String?,
    internal val jsonObject: JSONObject
) {

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
     * @return The identifiers of all the active entitlements
     */
    val activeEntitlements: Set<String>
        get() = activeIdentifiers(allExpirationDatesByEntitlement)

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
        return expirations.filterValues { date -> date == null || isAfterReferenceDate(date) }.keys
    }

    private fun isAfterReferenceDate(date: Date): Boolean {
        return date.after(requestDate?: Date())
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
            val nonSubscriptionPurchases = otherPurchases.keys().asSequence().toSet()
            val subscriptions = subscriber.getJSONObject("subscriptions")
            val expirationDatesByProduct = parseExpirations(subscriptions)

            var entitlements = JSONObject()
            if (subscriber.has("entitlements")) {
                entitlements = subscriber.getJSONObject("entitlements")
            }

            val expirationDatesByEntitlement = parseExpirations(entitlements)

            val requestDate =
                if (jsonObject.has("request_date")) {
                    try {
                        jsonObject.getString("request_date").takeUnless { it.isNullOrBlank() }
                            ?.let {
                                Iso8601Utils.parse(it)
                            }
                    } catch (e: RuntimeException) {
                        throw JSONException(e.message)
                    }
                } else null

            val aliasToken =
                if (subscriber.has("alias_token")) {
                    try {
                        subscriber.getString("alias_token")
                    } catch (e: java.lang.RuntimeException) {
                        throw JSONException(e.message)
                    }
                } else null

            return PurchaserInfo(
                nonSubscriptionPurchases,
                expirationDatesByProduct,
                expirationDatesByEntitlement,
                requestDate,
                aliasToken,
                jsonObject
            )
        }
    }

}
