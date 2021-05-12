package com.revenuecat.purchases.subscriberattributes

import com.revenuecat.purchases.common.attribution.AttributionNetwork
import com.revenuecat.purchases.utils.optNullableString
import org.json.JSONObject

class AttributionDataMigrator {

    fun convertAttributionDataToSubscriberAttributes(
        data: JSONObject,
        network: AttributionNetwork
    ): Map<String, String> {
        val mappingOfCommonAttribution: Map<Any, String> = mapOf(
            "rc_idfa" to SPECIAL_KEY_IDFA,
            "rc_idfv" to SPECIAL_KEY_IDFV,
            "rc_ip_address" to SPECIAL_KEY_IP,
            "rc_gps_adid" to SPECIAL_KEY_GPS_AD_ID
        )

        val commonSubscriberAttributes = data.convertToSubscriberAttributes(mappingOfCommonAttribution)

        val networkSpecificSubscriberAttributes: Map<String, String> = when (network) {
            AttributionNetwork.ADJUST -> {
                val mapping: Map<Any, String> = mapOf(
                    ("adid" or "rc_attribution_network_id") to SPECIAL_KEY_ADJUST_ID,
                    "network" to SPECIAL_KEY_MEDIA_SOURCE,
                    "campaign" to SPECIAL_KEY_CAMPAIGN,
                    "adgroup" to SPECIAL_KEY_AD_GROUP,
                    "creative" to SPECIAL_KEY_CREATIVE,
                )
                data.convertToSubscriberAttributes(mapping)
            }
            AttributionNetwork.APPSFLYER -> {
                val innerDataJSONObject = data.optJSONObject("data")
                if (data.has("status") && innerDataJSONObject != null) {
                    val keys = innerDataJSONObject.keys()

                    while (keys.hasNext()) {
                        val key = keys.next()
                        data.put(key, innerDataJSONObject[key])
                    }
                }
                val mapping: Map<Any, String> = mapOf(
                    ("rc_appsflyer_id" or "rc_attribution_network_id") to SPECIAL_KEY_APPSFLYER_ID,
                    ("af_channel" or "media_source") to SPECIAL_KEY_MEDIA_SOURCE,
                    "campaign" to SPECIAL_KEY_CAMPAIGN,
                    "adset" to SPECIAL_KEY_AD_GROUP,
                    ("af_ad" or "adgroup") to SPECIAL_KEY_AD,
                    "af_keywords" to SPECIAL_KEY_KEYWORD,
                    "ad_id" to SPECIAL_KEY_CREATIVE
                )
                data.convertToSubscriberAttributes(mapping)
            }
            AttributionNetwork.BRANCH -> {
                val mapping: Map<Any, String> = mapOf(
                    "channel" to SPECIAL_KEY_MEDIA_SOURCE,
                    "campaign" to SPECIAL_KEY_CAMPAIGN
                )
                data.convertToSubscriberAttributes(mapping)
            }
            AttributionNetwork.MPARTICLE -> {
                val mapping: Map<Any, String> = mapOf(
                    ("mpid" or "rc_attribution_network_id") to SPECIAL_KEY_MPARTICLE_ID,
                )
                data.convertToSubscriberAttributes(mapping)
            }
            AttributionNetwork.TENJIN,
            AttributionNetwork.FACEBOOK -> { emptyMap() }
        }

        return commonSubscriberAttributes + networkSpecificSubscriberAttributes
    }

    private fun JSONObject.convertToSubscriberAttributes(
        mapping: Map<Any, String>
    ): Map<String, String> {
        val subscriberAttributes = mutableMapOf<String, String>()
        mapping.forEach { (attributionKey, specialSubscriberAttribute) ->
            when (attributionKey) {
                is String -> {
                    this.optNullableString(attributionKey)?.let { subscriberAttributes[specialSubscriberAttribute] = it }
                }
                is Pair<*, *> -> {
                    (this.optNullableString(attributionKey.first as String) ?: this.optNullableString(attributionKey.second as String))?.let {
                        subscriberAttributes[specialSubscriberAttribute] = it
                    }
                }
            }
        }
        return subscriberAttributes
    }

    private infix fun <A, B> A.or(that: B): Pair<A, B> = to(that)

}