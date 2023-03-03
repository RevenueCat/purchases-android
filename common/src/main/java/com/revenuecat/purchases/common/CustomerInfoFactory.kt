package com.revenuecat.purchases.common

import android.net.Uri
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.EntitlementInfos
import com.revenuecat.purchases.VerificationResult
import com.revenuecat.purchases.common.networking.HTTPResult
import com.revenuecat.purchases.utils.Iso8601Utils
import com.revenuecat.purchases.utils.optNullableString
import com.revenuecat.purchases.utils.parseExpirations
import com.revenuecat.purchases.utils.parsePurchaseDates
import org.json.JSONException
import org.json.JSONObject
import java.util.Collections.emptyMap

/**
 * Builds a CustomerInfo
 * @throws [JSONException] If the json is invalid.
 */
object CustomerInfoFactory {
    @Throws(JSONException::class)
    fun buildCustomerInfo(httpResult: HTTPResult): CustomerInfo {
        return buildCustomerInfo(httpResult.body, httpResult.verificationResult)
    }

    @Throws(JSONException::class)
    fun buildCustomerInfo(body: JSONObject, verificationResult: VerificationResult): CustomerInfo {
        val subscriber = body.getJSONObject("subscriber")

        val nonSubscriptions = subscriber.getJSONObject("non_subscriptions")
        val nonSubscriptionsLatestPurchases = JSONObject()
        nonSubscriptions.keys().forEach { productId ->
            val arrayOfNonSubscriptions = nonSubscriptions.getJSONArray(productId)
            val numberOfNonSubscriptions = arrayOfNonSubscriptions.length()
            if (numberOfNonSubscriptions > 0) {
                nonSubscriptionsLatestPurchases.put(
                    productId,
                    arrayOfNonSubscriptions.getJSONObject(numberOfNonSubscriptions - 1)
                )
            }
        }

        val subscriptions = subscriber.getJSONObject("subscriptions")
        val expirationDatesByProduct = subscriptions.parseExpirations()
        val purchaseDatesByProduct =
            subscriptions.parsePurchaseDates() + nonSubscriptionsLatestPurchases.parsePurchaseDates()

        val entitlements = subscriber.optJSONObject("entitlements")

        val requestDate = Iso8601Utils.parse(body.getString("request_date"))

        val firstSeen = Iso8601Utils.parse(subscriber.getString("first_seen"))

        val entitlementInfos = entitlements?.buildEntitlementInfos(
            subscriptions,
            nonSubscriptionsLatestPurchases,
            requestDate,
            verificationResult
        ) ?: EntitlementInfos(emptyMap(), VerificationResult.NOT_REQUESTED)

        val managementURL = subscriber.optNullableString("management_url")
        val originalPurchaseDate = subscriber.optNullableString("original_purchase_date")?.let {
            Iso8601Utils.parse(it) ?: null
        }

        return CustomerInfo(
            entitlements = entitlementInfos,
            purchasedNonSubscriptionSkus = nonSubscriptions.keys().asSequence().toSet(),
            allExpirationDatesByProduct = expirationDatesByProduct,
            allPurchaseDatesByProduct = purchaseDatesByProduct,
            requestDate = requestDate,
            jsonObject = body,
            schemaVersion = body.optInt("schema_version"),
            firstSeen = firstSeen,
            originalAppUserId = subscriber.optString("original_app_user_id"),
            managementURL = managementURL?.let { Uri.parse(it) },
            originalPurchaseDate = originalPurchaseDate
        )
    }
}
