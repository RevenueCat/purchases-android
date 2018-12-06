package com.revenuecat.purchases

import com.revenuecat.purchases.util.Iso8601Utils

import org.json.JSONException
import org.json.JSONObject

import java.util.Date
import java.util.HashMap

/**
 * Class containing all information regarding the purchaser
 * @property purchasedNonSubscriptionSkus Set of non-subscription, non-consumed skus
 * @property allExpirationDatesByProduct Map of skus to expiration dates
 * @property allPurchaseDatesByProduct Map of skus to purchase dates
 * @property allExpirationDatesByEntitlement Map of entitlement ids to expiration dates
 * @property allPurchaseDatesByEntitlement Map of entitlement ids to purchase dates
 * @property requestDate Date when this info was requested
 */
class PurchaserInfo private constructor(
    val purchasedNonSubscriptionSkus: Set<String>,
    val allExpirationDatesByProduct: Map<String, Date?>,
    val allPurchaseDatesByProduct: Map<String, Date?>,
    val allExpirationDatesByEntitlement: Map<String, Date?>,
    val allPurchaseDatesByEntitlement: Map<String, Date?>,
    val requestDate: Date?,
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
        return expirations.filterValues { date -> date == null || isAfterReferenceDate(date) }.keys
    }

    private fun isAfterReferenceDate(date: Date): Boolean {
        return date.after(requestDate?: Date())
    }

    internal object Factory {

        /**
         * Parses expiration dates in a JSONObject
         * @param jsonObject JSONObject to deserialize
         * @throws [JSONException] If the json is invalid.
         */
        private fun parseExpirations(expirations: JSONObject): Map<String, Date?> {
            return parseDates(expirations, "expires_date")
        }

        /**
         * Parses purchase dates in a JSONObject
         * @param jsonObject JSONObject to deserialize
         * @throws [JSONException] If the json is invalid.
         */
        private fun parsePurchaseDates(expirations: JSONObject): Map<String, Date?> {
            return parseDates(expirations, "purchase_date")
        }

        /**
         * Parses dates that match a JSON key in a JSONObject
         * @param jsonObject JSONObject to deserialize
         * @param jsonKey Key of the dates to deserialize from the JSONObject
         * @throws [JSONException] If the json is invalid.
         */
        private fun parseDates(dates: JSONObject, jsonKey: String): HashMap<String, Date?> {
            val expirationDates = HashMap<String, Date?>()

            val it = dates.keys()
            while (it.hasNext()) {
                val key = it.next()

                val expirationObject = dates.getJSONObject(key)

                if (expirationObject.isNull(jsonKey)) {
                    expirationDates[key] = null
                } else {
                    val dateValue = expirationObject.getString(jsonKey)
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
            val purchaseDatesByProduct = parsePurchaseDates(subscriptions)

            var entitlements = JSONObject()
            if (subscriber.has("entitlements")) {
                entitlements = subscriber.getJSONObject("entitlements")
            }

            val expirationDatesByEntitlement = parseExpirations(entitlements)
            val purchaseDatesByEntitlement = parsePurchaseDates(entitlements)

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

            return PurchaserInfo(
                nonSubscriptionPurchases,
                expirationDatesByProduct,
                purchaseDatesByProduct,
                expirationDatesByEntitlement,
                purchaseDatesByEntitlement,
                requestDate,
                jsonObject
            )
        }
    }

}
