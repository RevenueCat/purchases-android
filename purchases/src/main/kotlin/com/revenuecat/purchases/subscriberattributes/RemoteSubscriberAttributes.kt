@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.subscriberattributes

import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.common.responses.CustomerInfoResponseJsonKeys
import com.revenuecat.purchases.common.warnLog
import com.revenuecat.purchases.strings.AttributionStrings
import com.revenuecat.purchases.subscriberattributes.caching.SubscriberAttributeMap
import org.json.JSONObject
import java.util.Date

internal const val SUBSCRIBER_ATTRIBUTES_KEY = "subscriber_attributes"

/**
 * The subscriber attributes a CustomerInfo response carried, marked as synced.
 *
 * @property unreadableKeys keys the response contained but whose entries couldn't be parsed. Merging leaves the
 * local copies of these alone, since it can't tell what the server says about them.
 */
internal class RemoteSubscriberAttributes(
    val attributes: SubscriberAttributeMap,
    val unreadableKeys: Set<String> = emptySet(),
)

/**
 * Null when the response has no `subscriber_attributes` object (the backend only sends one for some users),
 * so callers can tell "no information" apart from "the server has no attributes".
 */
@Suppress("ReturnCount")
internal fun CustomerInfo.remoteSubscriberAttributes(): RemoteSubscriberAttributes? {
    val subscriber = rawData.optJSONObject(CustomerInfoResponseJsonKeys.SUBSCRIBER) ?: return null
    if (subscriber.isNull(SUBSCRIBER_ATTRIBUTES_KEY)) return null
    val attributesJSON = subscriber.optJSONObject(SUBSCRIBER_ATTRIBUTES_KEY) ?: run {
        warnLog { AttributionStrings.REMOTE_ATTRIBUTES_UNREADABLE }
        return null
    }

    val attributes = mutableMapOf<String, SubscriberAttribute>()
    val unreadableKeys = mutableSetOf<String>()
    attributesJSON.keys().forEach { key ->
        val attribute = attributesJSON.optJSONObject(key)?.toRemoteSubscriberAttribute(key)
        if (attribute != null) {
            attributes[key] = attribute
        } else {
            unreadableKeys += key
        }
    }
    if (unreadableKeys.isNotEmpty()) {
        warnLog { AttributionStrings.REMOTE_ATTRIBUTE_ENTRIES_UNREADABLE.format(unreadableKeys.sorted()) }
    }
    return RemoteSubscriberAttributes(attributes, unreadableKeys)
}

@Suppress("ReturnCount")
private fun JSONObject.toRemoteSubscriberAttribute(key: String): SubscriberAttribute? {
    if (!has(BACKEND_NAME_VALUE)) return null
    val value = when (val rawValue = opt(BACKEND_NAME_VALUE)) {
        JSONObject.NULL -> null
        is String -> rawValue
        else -> return null
    }
    val updatedAtMs = (opt(BACKEND_NAME_TIMESTAMP) as? Number)?.toLong() ?: return null
    return SubscriberAttribute(key, value, setTime = Date(updatedAtMs), isSynced = true)
}

/**
 * Reconciles locally stored attributes with the server's. The server's copy wins unless the local one is newer,
 * which keeps changes that haven't reached it yet. A synced local attribute the server no longer has was deleted
 * there and is dropped; an unsynced one is kept so it can still be posted.
 */
internal fun mergeSubscriberAttributes(
    local: SubscriberAttributeMap,
    remote: RemoteSubscriberAttributes,
): SubscriberAttributeMap {
    val merged = remote.attributes.toMutableMap()
    local.forEach { (key, localAttribute) ->
        val remoteAttribute = remote.attributes[key]
        val keepLocal = when {
            remoteAttribute != null -> localAttribute.setTime > remoteAttribute.setTime
            key in remote.unreadableKeys -> true
            else -> !localAttribute.isSynced
        }
        if (keepLocal) merged[key] = localAttribute
    }
    return merged
}
