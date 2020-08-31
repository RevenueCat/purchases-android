package com.revenuecat.purchases.subscriberattributes.caching

import org.json.JSONObject

internal fun Map<AppUserID, SubscriberAttributeMap>.toJSONObject(): JSONObject {
    val attributesObject = JSONObject().also { attributesJSONObject ->
        this.forEach { (appUserID, subscriberAttributeMap) ->
            val jsonObject = JSONObject().also { userJSONObject ->
                subscriberAttributeMap.forEach { (key, subscriberAttribute) ->
                    userJSONObject.put(key, subscriberAttribute.toJSONObject())
                }
            }
            attributesJSONObject.put(appUserID, jsonObject)
        }
    }
    return JSONObject().also { it.put("attributes", attributesObject) }
}
