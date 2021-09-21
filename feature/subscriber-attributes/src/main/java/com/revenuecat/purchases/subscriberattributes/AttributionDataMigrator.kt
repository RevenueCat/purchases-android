package com.revenuecat.purchases.subscriberattributes

import com.revenuecat.purchases.common.attribution.AttributionNetwork
import com.revenuecat.purchases.common.subscriberattributes.SPECIAL_KEY_AD
import com.revenuecat.purchases.common.subscriberattributes.SPECIAL_KEY_ADJUST_ID
import com.revenuecat.purchases.common.subscriberattributes.SPECIAL_KEY_AD_GROUP
import com.revenuecat.purchases.common.subscriberattributes.SPECIAL_KEY_APPSFLYER_ID
import com.revenuecat.purchases.common.subscriberattributes.SPECIAL_KEY_CAMPAIGN
import com.revenuecat.purchases.common.subscriberattributes.SPECIAL_KEY_CREATIVE
import com.revenuecat.purchases.common.subscriberattributes.SPECIAL_KEY_GPS_AD_ID
import com.revenuecat.purchases.common.subscriberattributes.SPECIAL_KEY_IDFA
import com.revenuecat.purchases.common.subscriberattributes.SPECIAL_KEY_IDFV
import com.revenuecat.purchases.common.subscriberattributes.SPECIAL_KEY_IP
import com.revenuecat.purchases.common.subscriberattributes.SPECIAL_KEY_KEYWORD
import com.revenuecat.purchases.common.subscriberattributes.SPECIAL_KEY_MEDIA_SOURCE
import com.revenuecat.purchases.common.subscriberattributes.SPECIAL_KEY_MPARTICLE_ID
import com.revenuecat.purchases.utils.optNullableString
import org.json.JSONObject

internal object AttributionKeys {
    const val IDFA = "rc_idfa"
    const val IDFV = "rc_idfv"
    const val IP = "rc_ip_address"
    const val GPS_AD_ID = "rc_gps_adid"
    const val NETWORK_ID = "rc_attribution_network_id"
    internal object Adjust {
        const val ID = "adid"
        const val NETWORK = "network"
        const val CAMPAIGN = "campaign"
        const val AD_GROUP = "adgroup"
        const val CREATIVE = "creative"
    }
    internal object AppsFlyer {
        const val ID = "rc_appsflyer_id"
        const val CAMPAIGN = "campaign"
        const val CHANNEL = "af_channel"
        const val MEDIA_SOURCE = "media_source"
        const val ADSET = "adset"
        const val AD = "af_ad"
        const val AD_GROUP = "adgroup"
        const val AD_KEYWORDS = "af_keywords"
        const val AD_ID = "ad_id"
        const val DATA_KEY = "data"
        const val STATUS_KEY = "status"
    }
    internal object Branch {
        const val CAMPAIGN = "campaign"
        const val CHANNEL = "channel"
    }
    internal object MParticle {
        const val ID = "mpid"
    }
}

class AttributionDataMigrator {

    fun convertAttributionDataToSubscriberAttributes(
        data: JSONObject,
        network: AttributionNetwork
    ): Map<String, String> {
        val mappingOfCommonAttribution: Map<Any, String> = mapOf(
            AttributionKeys.IDFA to SPECIAL_KEY_IDFA,
            AttributionKeys.IDFV to SPECIAL_KEY_IDFV,
            AttributionKeys.IP to SPECIAL_KEY_IP,
            AttributionKeys.GPS_AD_ID to SPECIAL_KEY_GPS_AD_ID
        )

        val commonSubscriberAttributes = data.convertToSubscriberAttributes(mappingOfCommonAttribution)

        val networkSpecificSubscriberAttributes: Map<String, String> = when (network) {
            AttributionNetwork.ADJUST -> {
                convertAdjustAttribution(data)
            }
            AttributionNetwork.APPSFLYER -> {
                convertAppsFlyerAttribution(data)
            }
            AttributionNetwork.BRANCH -> {
                convertBranchAttribution(data)
            }
            AttributionNetwork.MPARTICLE -> {
                convertMParticleAttribution(data)
            }
            AttributionNetwork.TENJIN,
            AttributionNetwork.FACEBOOK -> { emptyMap() }
        }

        return commonSubscriberAttributes + networkSpecificSubscriberAttributes
    }

    private fun convertMParticleAttribution(data: JSONObject): Map<String, String> {
        val mapping: Map<Any, String> = mapOf(
            (AttributionKeys.NETWORK_ID or AttributionKeys.MParticle.ID) to SPECIAL_KEY_MPARTICLE_ID,
        )
        return data.convertToSubscriberAttributes(mapping)
    }

    private fun convertBranchAttribution(data: JSONObject): Map<String, String> {
        val mapping: Map<Any, String> = mapOf(
            AttributionKeys.Branch.CHANNEL to SPECIAL_KEY_MEDIA_SOURCE,
            AttributionKeys.Branch.CAMPAIGN to SPECIAL_KEY_CAMPAIGN
        )
        return data.convertToSubscriberAttributes(mapping)
    }

    private fun convertAppsFlyerAttribution(data: JSONObject): Map<String, String> {
        val innerDataJSONObject = data.optJSONObject(AttributionKeys.AppsFlyer.DATA_KEY)
        if (data.has(AttributionKeys.AppsFlyer.STATUS_KEY) && innerDataJSONObject != null) {
            val keys = innerDataJSONObject.keys()

            while (keys.hasNext()) {
                val key = keys.next()
                data.put(key, innerDataJSONObject[key])
            }
        }
        val mapping: Map<Any, String> = mapOf(
            (AttributionKeys.AppsFlyer.ID or AttributionKeys.NETWORK_ID) to SPECIAL_KEY_APPSFLYER_ID,
            (AttributionKeys.AppsFlyer.CHANNEL or AttributionKeys.AppsFlyer.MEDIA_SOURCE)
                to SPECIAL_KEY_MEDIA_SOURCE,
            AttributionKeys.AppsFlyer.CAMPAIGN to SPECIAL_KEY_CAMPAIGN,
            AttributionKeys.AppsFlyer.ADSET to SPECIAL_KEY_AD_GROUP,
            (AttributionKeys.AppsFlyer.AD or AttributionKeys.AppsFlyer.AD_GROUP) to SPECIAL_KEY_AD,
            AttributionKeys.AppsFlyer.AD_KEYWORDS to SPECIAL_KEY_KEYWORD,
            AttributionKeys.AppsFlyer.AD_ID to SPECIAL_KEY_CREATIVE
        )
        return data.convertToSubscriberAttributes(mapping)
    }

    private fun convertAdjustAttribution(data: JSONObject): Map<String, String> {
        val mapping: Map<Any, String> = mapOf(
            (AttributionKeys.Adjust.ID or AttributionKeys.NETWORK_ID) to SPECIAL_KEY_ADJUST_ID,
            AttributionKeys.Adjust.NETWORK to SPECIAL_KEY_MEDIA_SOURCE,
            AttributionKeys.Adjust.CAMPAIGN to SPECIAL_KEY_CAMPAIGN,
            AttributionKeys.Adjust.AD_GROUP to SPECIAL_KEY_AD_GROUP,
            AttributionKeys.Adjust.CREATIVE to SPECIAL_KEY_CREATIVE,
        )
        return data.convertToSubscriberAttributes(mapping)
    }

    private fun JSONObject.convertToSubscriberAttributes(
        mapping: Map<Any, String>
    ): Map<String, String> {
        val subscriberAttributes = mutableMapOf<String, String>()
        mapping.forEach { (attributionKey, specialSubscriberAttribute) ->
            when (attributionKey) {
                is String -> {
                    this.optNullableString(attributionKey)?.let {
                        subscriberAttributes[specialSubscriberAttribute] = it
                    }
                }
                is Pair<*, *> -> {
                    val firstOption = this.optNullableString(attributionKey.first as String)
                    val secondOption = this.optNullableString(attributionKey.second as String)
                    (firstOption ?: secondOption)?.let { subscriberAttributes[specialSubscriberAttribute] = it }
                }
            }
        }
        return subscriberAttributes
    }

    private infix fun <A, B> A.or(that: B): Pair<A, B> = to(that)
}
