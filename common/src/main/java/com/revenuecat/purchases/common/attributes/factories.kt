package com.revenuecat.purchases.common.attributes

import com.revenuecat.purchases.common.caching.SubscriberAttributeMap
import com.revenuecat.purchases.common.caching.SubscriberAttributesPerAppUserIDMap
import org.json.JSONObject

fun JSONObject.buildLegacySubscriberAttributes(): Map<String, SubscriberAttribute> {
    val attributesJSONObject = getJSONObject("attributes")
    return attributesJSONObject.buildSubscriberAttributesMap()
}

fun JSONObject.buildSubscriberAttributesMapPerUser(): SubscriberAttributesPerAppUserIDMap {
    val attributesJSONObject = getJSONObject("attributes")
    return attributesJSONObject.keys().asSequence().map { userId ->
        val attributesForUser = attributesJSONObject[userId] as JSONObject
        userId to attributesForUser.buildSubscriberAttributesMap()
    }.toMap()
}

fun JSONObject.buildSubscriberAttributesMap(): SubscriberAttributeMap {
    return this.keys().asSequence().map { attributeKey ->
        val attributeJSONObject = this[attributeKey] as JSONObject
        attributeKey to SubscriberAttribute(attributeJSONObject)
    }.toMap()
}
