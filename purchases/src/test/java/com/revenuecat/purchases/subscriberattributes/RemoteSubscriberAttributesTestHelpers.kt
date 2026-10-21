package com.revenuecat.purchases.subscriberattributes

import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.CustomerInfoOriginalSource
import com.revenuecat.purchases.VerificationResult
import com.revenuecat.purchases.common.CustomerInfoFactory
import org.json.JSONObject
import java.util.Date

internal const val REMOTE_SET_TIME_MS = 1_600_000_000_000L
internal val remoteSetTime = Date(REMOTE_SET_TIME_MS)

/**
 * [subscriberAttributes] is written as-is under `subscriber.subscriber_attributes`, so it can be malformed.
 * Null leaves the key out.
 */
internal fun customerInfoWithSubscriberAttributes(
    subscriberAttributes: Any?,
    originalSource: CustomerInfoOriginalSource = CustomerInfoOriginalSource.MAIN,
): CustomerInfo {
    val subscriber = JSONObject(
        """
        {
          "first_seen": "2019-07-17T00:05:54Z",
          "original_app_user_id": "app_user_id",
          "subscriptions": {},
          "non_subscriptions": {},
          "other_purchases": {}
        }
        """.trimIndent(),
    )
    subscriberAttributes?.let { subscriber.put(SUBSCRIBER_ATTRIBUTES_KEY, it) }
    val body = JSONObject().put("request_date", "2019-08-16T10:30:42Z").put("subscriber", subscriber)
    return CustomerInfoFactory.buildCustomerInfo(
        body,
        overrideRequestDate = null,
        verificationResult = VerificationResult.NOT_REQUESTED,
        originalSource = originalSource,
    )
}

internal fun remoteAttributesJSON(vararg entries: Pair<String, Any?>): JSONObject =
    JSONObject().apply { entries.forEach { (key, entry) -> put(key, entry) } }

internal fun remoteEntry(value: Any?, updatedAtMs: Any? = REMOTE_SET_TIME_MS): JSONObject =
    JSONObject().apply {
        put("value", value ?: JSONObject.NULL)
        updatedAtMs?.let { put("updated_at_ms", it) }
    }

internal fun attribute(key: String, value: String?, synced: Boolean, at: Date) =
    SubscriberAttribute(key, value, setTime = at, isSynced = synced)
