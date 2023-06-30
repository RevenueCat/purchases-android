package com.revenuecat.purchases.subscriberattributes

import com.revenuecat.purchases.subscriberattributes.caching.SubscriberAttributeMap
import com.revenuecat.purchases.subscriberattributes.caching.SubscriberAttributesPerAppUserIDMap
import org.json.JSONObject

internal fun JSONObject.buildLegacySubscriberAttributes(): Map<String, SubscriberAttribute> {
    val attributesJSONObject = getJSONObject("attributes")
    return attributesJSONObject.buildSubscriberAttributesMap()
}

internal fun JSONObject.buildSubscriberAttributesMapPerUser(): SubscriberAttributesPerAppUserIDMap {
    val attributesJSONObject = getJSONObject("attributes")
    return attributesJSONObject.keys().asSequence().map { userId ->
        val attributesForUser = attributesJSONObject[userId] as JSONObject
        userId to attributesForUser.buildSubscriberAttributesMap()
    }.toMap()
}

internal fun JSONObject.buildSubscriberAttributesMap(): SubscriberAttributeMap {
    return this.keys().asSequence().map { attributeKey ->
        val attributeJSONObject = this[attributeKey] as JSONObject
        attributeKey to SubscriberAttribute(attributeJSONObject)
    }.toMap()
}
