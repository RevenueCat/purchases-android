package com.revenuecat.purchases.common

import android.net.Uri
import androidx.annotation.VisibleForTesting
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.EntitlementInfos
import com.revenuecat.purchases.SubscriptionInfo
import com.revenuecat.purchases.VerificationResult
import com.revenuecat.purchases.common.caching.CUSTOMER_INFO_SCHEMA_VERSION
import com.revenuecat.purchases.common.networking.HTTPResult
import com.revenuecat.purchases.common.responses.CustomerInfoResponseJsonKeys
import com.revenuecat.purchases.common.responses.ProductResponseJsonKeys
import com.revenuecat.purchases.common.responses.SubscriptionInfoResponse
import com.revenuecat.purchases.utils.Iso8601Utils
import com.revenuecat.purchases.utils.SerializationException
import com.revenuecat.purchases.utils.optDate
import com.revenuecat.purchases.utils.optNullableString
import kotlinx.serialization.json.Json
import org.json.JSONException
import org.json.JSONObject
import java.util.Collections.emptyMap
import java.util.Date

/**
 * Builds a CustomerInfo
 * @throws [JSONException] If the json is invalid.
 */
internal object CustomerInfoFactory {

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal val json = Json {
        ignoreUnknownKeys = true
    }

    @Throws(JSONException::class)
    fun buildCustomerInfo(httpResult: HTTPResult): CustomerInfo {
        return buildCustomerInfo(httpResult.body, httpResult.requestDate, httpResult.verificationResult)
    }

    @Throws(JSONException::class)
    fun buildCustomerInfo(
        body: JSONObject,
        overrideRequestDate: Date?,
        verificationResult: VerificationResult,
    ): CustomerInfo {
        val subscriber = body.getJSONObject(CustomerInfoResponseJsonKeys.SUBSCRIBER)

        val nonSubscriptions = subscriber.getJSONObject(CustomerInfoResponseJsonKeys.NON_SUBSCRIPTIONS)
        val nonSubscriptionsLatestPurchases = JSONObject()
        nonSubscriptions.keys().forEach { productId ->
            val arrayOfNonSubscriptions = nonSubscriptions.getJSONArray(productId)
            val numberOfNonSubscriptions = arrayOfNonSubscriptions.length()
            if (numberOfNonSubscriptions > 0) {
                nonSubscriptionsLatestPurchases.put(
                    productId,
                    arrayOfNonSubscriptions.getJSONObject(numberOfNonSubscriptions - 1),
                )
            }
        }

        val subscriptions = subscriber.getJSONObject(CustomerInfoResponseJsonKeys.SUBSCRIPTIONS)
        val expirationDatesByProduct = subscriptions.parseExpirations()
        val purchaseDatesByProduct =
            subscriptions.parsePurchaseDates() + nonSubscriptionsLatestPurchases.parsePurchaseDates()

        val entitlements = subscriber.optJSONObject(CustomerInfoResponseJsonKeys.ENTITLEMENTS)

        val requestDate =
            overrideRequestDate ?: Iso8601Utils.parse(body.getString(CustomerInfoResponseJsonKeys.REQUEST_DATE))

        val firstSeen = Iso8601Utils.parse(subscriber.getString(CustomerInfoResponseJsonKeys.FIRST_SEEN))

        val entitlementInfos = entitlements?.buildEntitlementInfos(
            subscriptions,
            nonSubscriptionsLatestPurchases,
            requestDate,
            verificationResult,
        ) ?: EntitlementInfos(
            emptyMap(),
            verificationResult,
        )

        val managementURL = subscriber.optNullableString(CustomerInfoResponseJsonKeys.MANAGEMENT_URL)
        val originalPurchaseDate =
            subscriber.optNullableString(CustomerInfoResponseJsonKeys.ORIGINAL_PURCHASE_DATE)?.let {
                Iso8601Utils.parse(it) ?: null
            }

        return CustomerInfo(
            entitlements = entitlementInfos,
            allExpirationDatesByProduct = expirationDatesByProduct,
            allPurchaseDatesByProduct = purchaseDatesByProduct,
            requestDate = requestDate,
            jsonObject = body,
            schemaVersion = body.optInt("schema_version", CUSTOMER_INFO_SCHEMA_VERSION),
            firstSeen = firstSeen,
            originalAppUserId = subscriber.optString(CustomerInfoResponseJsonKeys.ORIGINAL_APP_USER_ID),
            managementURL = managementURL?.let { Uri.parse(it) },
            originalPurchaseDate = originalPurchaseDate,
        )
    }

    fun parseSubscriptionInfos(subscriberJSONObject: JSONObject, requestDate: Date): Map<String, SubscriptionInfo> {
        val subscriptionMap = mutableMapOf<String, SubscriptionInfo>()
        val subscriptions = subscriberJSONObject.getJSONObject("subscriptions")
        try {
            subscriptions.keys().forEach { productId ->
                val subscriptionJSONObject = subscriptions.getJSONObject(productId)
                val subscriptionInfoResponse = json.decodeFromString<SubscriptionInfoResponse>(
                    subscriptionJSONObject.toString(),
                )
                subscriptionMap[productId] = SubscriptionInfo(productId, requestDate, subscriptionInfoResponse)
            }
        } catch (s: SerializationException) {
            errorLog("Error deserializing subscription information", s)
            emptyMap<String, SubscriptionInfo>()
        } catch (i: IllegalArgumentException) {
            errorLog("Error deserializing subscription information. The input is not a SubscriptionInfo", i)
            emptyMap<String, SubscriptionInfo>()
        }
        return subscriptionMap
    }

    /**
     * Parses expiration dates in a JSONObject
     * @throws [JSONException] If the json is invalid.
     */
    private fun JSONObject.parseExpirations(): Map<String, Date?> {
        return parseDates(ProductResponseJsonKeys.EXPIRES_DATE)
    }

    /**
     * Parses purchase dates in a JSONObject
     * @throws [JSONException] If the json is invalid.
     */
    private fun JSONObject.parsePurchaseDates(): Map<String, Date?> {
        return parseDates(CustomerInfoResponseJsonKeys.PURCHASE_DATE)
    }

    /**
     * Parses dates that match a JSON key in a JSONObject
     * @param jsonKey Key of the dates to deserialize from the JSONObject
     * @throws [JSONException] If the json is invalid.
     */
    private fun JSONObject.parseDates(jsonKey: String): HashMap<String, Date?> {
        val expirationDates = HashMap<String, Date?>()

        val it = keys()
        while (it.hasNext()) {
            val productId = it.next()
            val basePlanId = getJSONObject(productId).optString(ProductResponseJsonKeys.PRODUCT_PLAN_IDENTIFIER)
                .takeIf { it.isNotEmpty() }

            val expirationObject = getJSONObject(productId)

            val key = basePlanId?.let { "$productId${Constants.SUBS_ID_BASE_PLAN_ID_SEPARATOR}$it" } ?: productId
            expirationDates[key] = expirationObject.optDate(jsonKey)
        }

        return expirationDates
    }
}
