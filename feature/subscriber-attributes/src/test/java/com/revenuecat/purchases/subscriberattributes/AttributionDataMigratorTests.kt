package com.revenuecat.purchases.subscriberattributes

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.common.attribution.AttributionNetwork
import org.assertj.core.api.Assertions.assertThat
import org.json.JSONObject
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AttributionDataMigratorTests {

    private val DEFAULT_IDFA = "00000000-0000-0000-0000-000000000000"
    private val DEFAULT_IDFV = "A9CFE78C-51F8-4808-94FD-56B4535753C6"
    private val DEFAULT_GPSADID = "e00000d0-0c0d-00b0-a000-acc0e00a0000"
    private val DEFAULT_IP = "192.168.1.130"

    private lateinit var underTest: AttributionDataMigrator

    @Before
    fun setup() {
        underTest = AttributionDataMigrator()
    }

    @Test
    fun `Adjust attribution is properly converted`() {
        val jsonObject = getAdjustJSON()
        val converted =
            underTest.convertAttributionDataToSubscriberAttributes(data = jsonObject, AttributionNetwork.ADJUST)
        assertThat(converted).isNotNull
        checkCommonAttributes(converted)
        assertThat(converted[SPECIAL_KEY_ADJUST_ID]).isEqualTo(jsonObject.getString(AttributionKeys.Adjust.ID))
        assertThat(converted[SPECIAL_KEY_MEDIA_SOURCE]).isEqualTo(jsonObject.getString(AttributionKeys.Adjust.NETWORK))
        assertThat(converted[SPECIAL_KEY_CAMPAIGN]).isEqualTo(jsonObject.getString(AttributionKeys.Adjust.CAMPAIGN))
        assertThat(converted[SPECIAL_KEY_AD_GROUP]).isEqualTo(jsonObject.getString(AttributionKeys.Adjust.AD_GROUP))
        assertThat(converted[SPECIAL_KEY_CREATIVE]).isEqualTo(jsonObject.getString(AttributionKeys.Adjust.CREATIVE))
    }

    @Test
    fun `Adjust attribution gives preference to adid over rc_attribution_network_id`() {
        val jsonObject = getAdjustJSON(
            addAdId = true,
            addNetworkID = true
        )
        val converted =
            underTest.convertAttributionDataToSubscriberAttributes(data = jsonObject, AttributionNetwork.ADJUST)
        assertThat(converted).isNotNull
        checkCommonAttributes(converted)
        assertThat(converted[SPECIAL_KEY_ADJUST_ID]).isEqualTo(jsonObject.getString(AttributionKeys.Adjust.ID))
        assertThat(converted[SPECIAL_KEY_MEDIA_SOURCE]).isEqualTo(jsonObject.getString(AttributionKeys.Adjust.NETWORK))
        assertThat(converted[SPECIAL_KEY_CAMPAIGN]).isEqualTo(jsonObject.getString(AttributionKeys.Adjust.CAMPAIGN))
        assertThat(converted[SPECIAL_KEY_AD_GROUP]).isEqualTo(jsonObject.getString(AttributionKeys.Adjust.AD_GROUP))
        assertThat(converted[SPECIAL_KEY_CREATIVE]).isEqualTo(jsonObject.getString(AttributionKeys.Adjust.CREATIVE))
    }

    @Test
    fun `Adjust attribution maps rc_attribution_network_id correctly`() {
        val jsonObject = getAdjustJSON(
            addAdId = false,
            addNetworkID = true
        )
        val converted =
            underTest.convertAttributionDataToSubscriberAttributes(data = jsonObject, AttributionNetwork.ADJUST)
        assertThat(converted).isNotNull
        checkCommonAttributes(converted)
        assertThat(converted[SPECIAL_KEY_ADJUST_ID]).isEqualTo(jsonObject.getString(AttributionKeys.NETWORK_ID))
        assertThat(converted[SPECIAL_KEY_MEDIA_SOURCE]).isEqualTo(jsonObject.getString(AttributionKeys.Adjust.NETWORK))
        assertThat(converted[SPECIAL_KEY_CAMPAIGN]).isEqualTo(jsonObject.getString(AttributionKeys.Adjust.CAMPAIGN))
        assertThat(converted[SPECIAL_KEY_AD_GROUP]).isEqualTo(jsonObject.getString(AttributionKeys.Adjust.AD_GROUP))
        assertThat(converted[SPECIAL_KEY_CREATIVE]).isEqualTo(jsonObject.getString(AttributionKeys.Adjust.CREATIVE))
    }

    @Test
    fun `Adjust attribution works with null rc_idfa`() {
        val jsonObject = getAdjustJSON(idfa = null)
        val converted =
            underTest.convertAttributionDataToSubscriberAttributes(data = jsonObject, AttributionNetwork.ADJUST)
        assertThat(converted).isNotNull
        checkCommonAttributes(converted, expectedIdfa = null)
        assertThat(converted[SPECIAL_KEY_ADJUST_ID]).isEqualTo(jsonObject.getString(AttributionKeys.Adjust.ID))
        assertThat(converted[SPECIAL_KEY_MEDIA_SOURCE]).isEqualTo(jsonObject.getString(AttributionKeys.Adjust.NETWORK))
        assertThat(converted[SPECIAL_KEY_CAMPAIGN]).isEqualTo(jsonObject.getString(AttributionKeys.Adjust.CAMPAIGN))
        assertThat(converted[SPECIAL_KEY_AD_GROUP]).isEqualTo(jsonObject.getString(AttributionKeys.Adjust.AD_GROUP))
        assertThat(converted[SPECIAL_KEY_CREATIVE]).isEqualTo(jsonObject.getString(AttributionKeys.Adjust.CREATIVE))
    }

    @Test
    fun `Adjust attribution works with null rc_idfv`() {
        val jsonObject = getAdjustJSON(idfv = null)
        val converted =
            underTest.convertAttributionDataToSubscriberAttributes(data = jsonObject, AttributionNetwork.ADJUST)
        assertThat(converted).isNotNull
        checkCommonAttributes(converted, expectedIdfv = null)
        assertThat(converted[SPECIAL_KEY_ADJUST_ID]).isEqualTo(jsonObject.getString(AttributionKeys.Adjust.ID))
        assertThat(converted[SPECIAL_KEY_MEDIA_SOURCE]).isEqualTo(jsonObject.getString(AttributionKeys.Adjust.NETWORK))
        assertThat(converted[SPECIAL_KEY_CAMPAIGN]).isEqualTo(jsonObject.getString(AttributionKeys.Adjust.CAMPAIGN))
        assertThat(converted[SPECIAL_KEY_AD_GROUP]).isEqualTo(jsonObject.getString(AttributionKeys.Adjust.AD_GROUP))
        assertThat(converted[SPECIAL_KEY_CREATIVE]).isEqualTo(jsonObject.getString(AttributionKeys.Adjust.CREATIVE))
    }

    @Test
    fun `AppsFlyer attribution is properly converted`() {
        val jsonObject = getAppsFlyerJSON()
        val converted =
            underTest.convertAttributionDataToSubscriberAttributes(data = jsonObject, AttributionNetwork.APPSFLYER)
        assertThat(converted).isNotNull
        checkCommonAttributes(converted)
        assertThat(converted[SPECIAL_KEY_APPSFLYER_ID]).isEqualTo(jsonObject.getString(AttributionKeys.AppsFlyer.ID))
        assertThat(converted[SPECIAL_KEY_MEDIA_SOURCE]).isEqualTo(jsonObject.getString(AttributionKeys.AppsFlyer.CHANNEL))
        assertThat(converted[SPECIAL_KEY_CAMPAIGN]).isEqualTo(jsonObject.getString(AttributionKeys.AppsFlyer.CAMPAIGN))
        assertThat(converted[SPECIAL_KEY_AD_GROUP]).isEqualTo(jsonObject.getString(AttributionKeys.AppsFlyer.ADSET))
        assertThat(converted[SPECIAL_KEY_AD]).isEqualTo(jsonObject.getString(AttributionKeys.AppsFlyer.AD))
        assertThat(converted[SPECIAL_KEY_KEYWORD]).isEqualTo(jsonObject.getString(AttributionKeys.AppsFlyer.AD_KEYWORDS))
        assertThat(converted[SPECIAL_KEY_CREATIVE]).isEqualTo(jsonObject.getString(AttributionKeys.AppsFlyer.AD_ID))
    }

    @Test
    fun `AppsFlyer attribution gives preference to adid over rc_attribution_network_id`() {
        val jsonObject = getAppsFlyerJSON(
            addAppsFlyerId = true,
            addNetworkID = true
        )
        val converted =
            underTest.convertAttributionDataToSubscriberAttributes(data = jsonObject, AttributionNetwork.APPSFLYER)
        assertThat(converted).isNotNull
        checkCommonAttributes(converted)
        assertThat(converted[SPECIAL_KEY_APPSFLYER_ID]).isEqualTo(jsonObject.getString(AttributionKeys.AppsFlyer.ID))
        assertThat(converted[SPECIAL_KEY_MEDIA_SOURCE]).isEqualTo(jsonObject.getString(AttributionKeys.AppsFlyer.CHANNEL))
        assertThat(converted[SPECIAL_KEY_CAMPAIGN]).isEqualTo(jsonObject.getString(AttributionKeys.AppsFlyer.CAMPAIGN))
        assertThat(converted[SPECIAL_KEY_AD_GROUP]).isEqualTo(jsonObject.getString(AttributionKeys.AppsFlyer.ADSET))
        assertThat(converted[SPECIAL_KEY_AD]).isEqualTo(jsonObject.getString(AttributionKeys.AppsFlyer.AD))
        assertThat(converted[SPECIAL_KEY_KEYWORD]).isEqualTo(jsonObject.getString(AttributionKeys.AppsFlyer.AD_KEYWORDS))
        assertThat(converted[SPECIAL_KEY_CREATIVE]).isEqualTo(jsonObject.getString(AttributionKeys.AppsFlyer.AD_ID))
    }

    @Test
    fun `AppsFlyer attribution maps rc_attribution_network_id correctly`() {
        val jsonObject = getAppsFlyerJSON(
            addAppsFlyerId = false,
            addNetworkID = true
        )
        val converted =
            underTest.convertAttributionDataToSubscriberAttributes(data = jsonObject, AttributionNetwork.APPSFLYER)
        assertThat(converted).isNotNull
        checkCommonAttributes(converted)
        assertThat(converted[SPECIAL_KEY_APPSFLYER_ID]).isEqualTo(jsonObject.getString(AttributionKeys.NETWORK_ID))
        assertThat(converted[SPECIAL_KEY_MEDIA_SOURCE]).isEqualTo(jsonObject.getString(AttributionKeys.AppsFlyer.CHANNEL))
        assertThat(converted[SPECIAL_KEY_CAMPAIGN]).isEqualTo(jsonObject.getString(AttributionKeys.AppsFlyer.CAMPAIGN))
        assertThat(converted[SPECIAL_KEY_AD_GROUP]).isEqualTo(jsonObject.getString(AttributionKeys.AppsFlyer.ADSET))
        assertThat(converted[SPECIAL_KEY_AD]).isEqualTo(jsonObject.getString(AttributionKeys.AppsFlyer.AD))
        assertThat(converted[SPECIAL_KEY_KEYWORD]).isEqualTo(jsonObject.getString(AttributionKeys.AppsFlyer.AD_KEYWORDS))
        assertThat(converted[SPECIAL_KEY_CREATIVE]).isEqualTo(jsonObject.getString(AttributionKeys.AppsFlyer.AD_ID))
    }

    @Test
    fun `AppsFlyer attribution works with null rc_idfa`() {
        val jsonObject = getAppsFlyerJSON(idfa = null)
        val converted =
            underTest.convertAttributionDataToSubscriberAttributes(data = jsonObject, AttributionNetwork.APPSFLYER)
        assertThat(converted).isNotNull
        checkCommonAttributes(converted, expectedIdfa = null)
        assertThat(converted[SPECIAL_KEY_APPSFLYER_ID]).isEqualTo(jsonObject.getString(AttributionKeys.AppsFlyer.ID))
        assertThat(converted[SPECIAL_KEY_MEDIA_SOURCE]).isEqualTo(jsonObject.getString(AttributionKeys.AppsFlyer.CHANNEL))
        assertThat(converted[SPECIAL_KEY_CAMPAIGN]).isEqualTo(jsonObject.getString(AttributionKeys.AppsFlyer.CAMPAIGN))
        assertThat(converted[SPECIAL_KEY_AD_GROUP]).isEqualTo(jsonObject.getString(AttributionKeys.AppsFlyer.ADSET))
        assertThat(converted[SPECIAL_KEY_AD]).isEqualTo(jsonObject.getString(AttributionKeys.AppsFlyer.AD))
        assertThat(converted[SPECIAL_KEY_KEYWORD]).isEqualTo(jsonObject.getString(AttributionKeys.AppsFlyer.AD_KEYWORDS))
        assertThat(converted[SPECIAL_KEY_CREATIVE]).isEqualTo(jsonObject.getString(AttributionKeys.AppsFlyer.AD_ID))
    }

    @Test
    fun `AppsFlyer attribution works with null rc_idfv`() {
        val jsonObject = getAppsFlyerJSON(idfv = null)
        val converted =
            underTest.convertAttributionDataToSubscriberAttributes(data = jsonObject, AttributionNetwork.APPSFLYER)
        assertThat(converted).isNotNull
        checkCommonAttributes(converted, expectedIdfv = null)
        assertThat(converted[SPECIAL_KEY_APPSFLYER_ID]).isEqualTo(jsonObject.getString(AttributionKeys.AppsFlyer.ID))
        assertThat(converted[SPECIAL_KEY_MEDIA_SOURCE]).isEqualTo(jsonObject.getString(AttributionKeys.AppsFlyer.CHANNEL))
        assertThat(converted[SPECIAL_KEY_CAMPAIGN]).isEqualTo(jsonObject.getString(AttributionKeys.AppsFlyer.CAMPAIGN))
        assertThat(converted[SPECIAL_KEY_AD_GROUP]).isEqualTo(jsonObject.getString(AttributionKeys.AppsFlyer.ADSET))
        assertThat(converted[SPECIAL_KEY_AD]).isEqualTo(jsonObject.getString(AttributionKeys.AppsFlyer.AD))
        assertThat(converted[SPECIAL_KEY_KEYWORD]).isEqualTo(jsonObject.getString(AttributionKeys.AppsFlyer.AD_KEYWORDS))
        assertThat(converted[SPECIAL_KEY_CREATIVE]).isEqualTo(jsonObject.getString(AttributionKeys.AppsFlyer.AD_ID))
    }

    @Test
    fun `AppsFlyer attribution maps media source correctly`() {
        var jsonObject = getAppsFlyerJSON(
            addChannel = false,
            addMediaSource = true
        )
        var converted =
            underTest.convertAttributionDataToSubscriberAttributes(data = jsonObject, AttributionNetwork.APPSFLYER)
        assertThat(converted).isNotNull
        checkCommonAttributes(converted)
        assertThat(converted[SPECIAL_KEY_APPSFLYER_ID]).isEqualTo(jsonObject.getString(AttributionKeys.AppsFlyer.ID))
        assertThat(converted[SPECIAL_KEY_MEDIA_SOURCE]).isEqualTo(jsonObject.getString(AttributionKeys.AppsFlyer.MEDIA_SOURCE))

        jsonObject = getAppsFlyerJSON(
            addChannel = true,
            addMediaSource = true
        )
        converted =
            underTest.convertAttributionDataToSubscriberAttributes(data = jsonObject, AttributionNetwork.APPSFLYER)
        assertThat(converted).isNotNull
        checkCommonAttributes(converted)
        assertThat(converted[SPECIAL_KEY_APPSFLYER_ID]).isEqualTo(jsonObject.getString(AttributionKeys.AppsFlyer.ID))
        assertThat(converted[SPECIAL_KEY_MEDIA_SOURCE]).isEqualTo(jsonObject.getString(AttributionKeys.AppsFlyer.CHANNEL))

        jsonObject = getAppsFlyerJSON(
            addChannel = true,
            addMediaSource = false
        )
        converted =
            underTest.convertAttributionDataToSubscriberAttributes(data = jsonObject, AttributionNetwork.APPSFLYER)
        assertThat(converted).isNotNull
        checkCommonAttributes(converted)
        assertThat(converted[SPECIAL_KEY_APPSFLYER_ID]).isEqualTo(jsonObject.getString(AttributionKeys.AppsFlyer.ID))
        assertThat(converted[SPECIAL_KEY_MEDIA_SOURCE]).isEqualTo(jsonObject.getString(AttributionKeys.AppsFlyer.CHANNEL))
    }

    @Test
    fun `AppsFlyer attribution maps ad correctly`() {
        var jsonObject = getAppsFlyerJSON(
            addAd = false,
            addAdGroup = true
        )
        var converted =
            underTest.convertAttributionDataToSubscriberAttributes(data = jsonObject, AttributionNetwork.APPSFLYER)
        assertThat(converted).isNotNull
        checkCommonAttributes(converted)
        assertThat(converted[SPECIAL_KEY_APPSFLYER_ID]).isEqualTo(jsonObject.getString(AttributionKeys.AppsFlyer.ID))
        assertThat(converted[SPECIAL_KEY_AD]).isEqualTo(jsonObject.getString(AttributionKeys.AppsFlyer.AD_GROUP))

        jsonObject = getAppsFlyerJSON(
            addAd = true,
            addAdGroup = true
        )
        converted =
            underTest.convertAttributionDataToSubscriberAttributes(data = jsonObject, AttributionNetwork.APPSFLYER)
        assertThat(converted).isNotNull
        checkCommonAttributes(converted)
        assertThat(converted[SPECIAL_KEY_APPSFLYER_ID]).isEqualTo(jsonObject.getString(AttributionKeys.AppsFlyer.ID))
        assertThat(converted[SPECIAL_KEY_AD]).isEqualTo(jsonObject.getString(AttributionKeys.AppsFlyer.AD))

        jsonObject = getAppsFlyerJSON(
            addAd = true,
            addAdGroup = false
        )
        converted =
            underTest.convertAttributionDataToSubscriberAttributes(data = jsonObject, AttributionNetwork.APPSFLYER)
        assertThat(converted).isNotNull
        checkCommonAttributes(converted)
        assertThat(converted[SPECIAL_KEY_APPSFLYER_ID]).isEqualTo(jsonObject.getString(AttributionKeys.AppsFlyer.ID))
        assertThat(converted[SPECIAL_KEY_AD]).isEqualTo(jsonObject.getString(AttributionKeys.AppsFlyer.AD))
    }

    @Test
    fun `AppsFlyer attribution is properly converted if it's inside data inner json`() {
        val jsonObject = JSONObject().also {
            it.put("status", 1)
        }
        val innerJSONObject = getAppsFlyerJSON()
        val innerJSONObjectClean = JSONObject()
        val keys = innerJSONObject.keys()

        while (keys.hasNext()) {
            val key = keys.next()
            val value = innerJSONObject.get(key)
            if (key.startsWith("rc_")) {
                jsonObject.put(key, value)
            } else {
                innerJSONObjectClean.put(key, value)
            }
        }

        jsonObject.put("data", innerJSONObjectClean)

        val converted =
            underTest.convertAttributionDataToSubscriberAttributes(data = jsonObject, AttributionNetwork.APPSFLYER)
        assertThat(converted).isNotNull
        checkCommonAttributes(converted)
        assertThat(converted[SPECIAL_KEY_APPSFLYER_ID]).isEqualTo(jsonObject.getString(AttributionKeys.AppsFlyer.ID))
        assertThat(converted[SPECIAL_KEY_MEDIA_SOURCE]).isEqualTo(jsonObject.getString(AttributionKeys.AppsFlyer.CHANNEL))
        assertThat(converted[SPECIAL_KEY_CAMPAIGN]).isEqualTo(jsonObject.getString(AttributionKeys.AppsFlyer.CAMPAIGN))
        assertThat(converted[SPECIAL_KEY_AD_GROUP]).isEqualTo(jsonObject.getString(AttributionKeys.AppsFlyer.ADSET))
        assertThat(converted[SPECIAL_KEY_AD]).isEqualTo(jsonObject.getString(AttributionKeys.AppsFlyer.AD))
        assertThat(converted[SPECIAL_KEY_KEYWORD]).isEqualTo(jsonObject.getString(AttributionKeys.AppsFlyer.AD_KEYWORDS))
        assertThat(converted[SPECIAL_KEY_CREATIVE]).isEqualTo(jsonObject.getString(AttributionKeys.AppsFlyer.AD_ID))
    }

    @Test
    fun `AppsFlyer attribution is properly converted if it's inside data inner json and uses rc_network_id`() {
        val jsonObject = JSONObject().also {
            it.put("status", 1)
        }
        val innerJSONObject = getAppsFlyerJSON(addAppsFlyerId = false, addNetworkID = true)
        val innerJSONObjectClean = JSONObject()
        val keys = innerJSONObject.keys()

        while (keys.hasNext()) {
            val key = keys.next()
            val value = innerJSONObject.get(key)
            if (key.startsWith("rc_")) {
                jsonObject.put(key, value)
            } else {
                innerJSONObjectClean.put(key, value)
            }
        }

        jsonObject.put("data", innerJSONObjectClean)

        val converted =
            underTest.convertAttributionDataToSubscriberAttributes(data = jsonObject, AttributionNetwork.APPSFLYER)
        assertThat(converted).isNotNull
        checkCommonAttributes(converted)
        assertThat(converted[SPECIAL_KEY_APPSFLYER_ID]).isEqualTo(jsonObject.getString(AttributionKeys.NETWORK_ID))
    }

    @Test
    fun `Branch attribution is properly converted`() {
        val jsonObject = getBranchJSON()
        val converted =
            underTest.convertAttributionDataToSubscriberAttributes(data = jsonObject, AttributionNetwork.BRANCH)
        assertThat(converted).isNotNull
        checkCommonAttributes(converted)
        assertThat(converted[SPECIAL_KEY_MEDIA_SOURCE]).isEqualTo(jsonObject.getString(AttributionKeys.Branch.CHANNEL))
        assertThat(converted[SPECIAL_KEY_CAMPAIGN]).isEqualTo(jsonObject.getString(AttributionKeys.Branch.CAMPAIGN))
    }

    @Test
    fun `Branch attribution works with null rc_idfa`() {
        val jsonObject = getBranchJSON(idfa = null)
        val converted =
            underTest.convertAttributionDataToSubscriberAttributes(data = jsonObject, AttributionNetwork.BRANCH)
        assertThat(converted).isNotNull
        checkCommonAttributes(converted, expectedIdfa = null)
        assertThat(converted[SPECIAL_KEY_MEDIA_SOURCE]).isEqualTo(jsonObject.getString(AttributionKeys.Branch.CHANNEL))
        assertThat(converted[SPECIAL_KEY_CAMPAIGN]).isEqualTo(jsonObject.getString(AttributionKeys.Branch.CAMPAIGN))
    }

    @Test
    fun `Branch attribution works with null rc_idfv`() {
        val jsonObject = getBranchJSON(idfv = null)
        val converted =
            underTest.convertAttributionDataToSubscriberAttributes(data = jsonObject, AttributionNetwork.BRANCH)
        assertThat(converted).isNotNull
        checkCommonAttributes(converted, expectedIdfv = null)
        assertThat(converted[SPECIAL_KEY_MEDIA_SOURCE]).isEqualTo(jsonObject.getString(AttributionKeys.Branch.CHANNEL))
        assertThat(converted[SPECIAL_KEY_CAMPAIGN]).isEqualTo(jsonObject.getString(AttributionKeys.Branch.CAMPAIGN))
    }

    @Test
    fun `Tenjin attribution is properly converted`() {
        val jsonObject = getFacebookOrTenjinJSON()
        val converted =
            underTest.convertAttributionDataToSubscriberAttributes(data = jsonObject, AttributionNetwork.TENJIN)
        assertThat(converted).isNotNull
        checkCommonAttributes(converted)
    }

    @Test
    fun `Tenjin attribution works with null rc_idfa`() {
        val jsonObject = getFacebookOrTenjinJSON(idfa = null)
        val converted =
            underTest.convertAttributionDataToSubscriberAttributes(data = jsonObject, AttributionNetwork.TENJIN)
        assertThat(converted).isNotNull
        checkCommonAttributes(converted, expectedIdfa = null)
    }

    @Test
    fun `Tenjin attribution works with null rc_idfv`() {
        val jsonObject = getFacebookOrTenjinJSON(idfv = null)
        val converted =
            underTest.convertAttributionDataToSubscriberAttributes(data = jsonObject, AttributionNetwork.TENJIN)
        assertThat(converted).isNotNull
        checkCommonAttributes(converted, expectedIdfv = null)
    }

    @Test
    fun `Facebook attribution is properly converted`() {
        val jsonObject = getFacebookOrTenjinJSON()
        val converted =
            underTest.convertAttributionDataToSubscriberAttributes(data = jsonObject, AttributionNetwork.FACEBOOK)
        assertThat(converted).isNotNull
        checkCommonAttributes(converted)
    }

    @Test
    fun `Facebook attribution works with null rc_idfa`() {
        val jsonObject = getFacebookOrTenjinJSON(idfa = null)
        val converted =
            underTest.convertAttributionDataToSubscriberAttributes(data = jsonObject, AttributionNetwork.FACEBOOK)
        assertThat(converted).isNotNull
        checkCommonAttributes(converted, expectedIdfa = null)
    }

    @Test
    fun `Facebook attribution works with null rc_idfv`() {
        val jsonObject = getFacebookOrTenjinJSON(idfv = null)
        val converted =
            underTest.convertAttributionDataToSubscriberAttributes(data = jsonObject, AttributionNetwork.FACEBOOK)
        assertThat(converted).isNotNull
        checkCommonAttributes(converted, expectedIdfv = null)
    }

    private fun getAdjustJSON(
        idfa: String? = DEFAULT_IDFA,
        addAdId: Boolean = true,
        addNetworkID: Boolean = false,
        idfv: String? = DEFAULT_IDFV
    ): JSONObject {
        return JSONObject(
            """
            {
                "clickLabel": "clickey",
                "trackerToken": "6abc940",
                "trackerName": "Instagram Profile::IG Spanish",
                ${if (addAdId) "\"${AttributionKeys.Adjust.ID}\": \"20f0c0000aca0b00000fb0000c0f0f00\"," else ""}
                ${if (addNetworkID) "\"${AttributionKeys.NETWORK_ID}\": \"10f0c0000aca0b00000fb0000c0f0f00\"," else ""}
                "${AttributionKeys.Adjust.CAMPAIGN}": "IG Spanish",
                "${AttributionKeys.Adjust.AD_GROUP}": "an_ad_group",
                "${AttributionKeys.Adjust.CREATIVE}": "a_creative",
                "${AttributionKeys.Adjust.NETWORK}": "Instagram Profile",
                "${AttributionKeys.GPS_AD_ID}": $DEFAULT_GPSADID,
                "${AttributionKeys.IP}": $DEFAULT_IP,
                "${AttributionKeys.IDFA}": ${idfa?.let { "$it" }},
                "${AttributionKeys.IDFV}": ${idfv?.let { "$it" }}
            }
        """.trimIndent()
        )
    }

    private fun getAppsFlyerJSON(
        idfa: String? = DEFAULT_IDFA,
        addAppsFlyerId: Boolean = true,
        addNetworkID: Boolean = false,
        idfv: String? = DEFAULT_IDFV,
        addChannel: Boolean = true,
        addMediaSource: Boolean = false,
        addAd: Boolean = true,
        addAdGroup: Boolean = false
    ): JSONObject {
        return JSONObject(
            """
            {
                "adset_id": "23847301359550211",
                "campaign_id": "23847301359200211",
                "click_time": "2021-05-04 18:08:51.000", 
                "iscache": false, 
                "adset": "0111 - mm - aaa - US - best 10", 
                "adgroup_id": "238473013556789090", 
                "is_mobile_data_terms_signed": true, 
                "match_type": "srn", 
                "agency": null, 
                "retargeting_conversion_type": "none", 
                "install_time": "2021-05-04 18:20:45.050", 
                "af_status": "Non-organic", 
                "http_referrer": null, 
                "is_paid": true, 
                "is_first_launch": false, 
                "is_fb": true, 
                "af_siteid": null,
                "af_message": "organic install",
                "${AttributionKeys.AppsFlyer.AD_ID}": "23847301457860211", 
                "${AttributionKeys.AppsFlyer.CAMPAIGN}": "0111 - mm - aaa - US - best creo 10 - Copy",
                ${if (addAppsFlyerId) "\"${AttributionKeys.AppsFlyer.ID}\": \"110116141-131918411\"," else ""}
                ${if (addNetworkID) "\"${AttributionKeys.NETWORK_ID}\": \"10f0c0000aca0b00000fb0000c0f0f00\"," else ""}
                ${if (addChannel) "\"${AttributionKeys.AppsFlyer.CHANNEL}\": \"Facebook\"," else ""}
                ${if (addMediaSource) "\"${AttributionKeys.AppsFlyer.MEDIA_SOURCE}\": \"Facebook Ads\"," else ""}
                ${if (addAd) "\"${AttributionKeys.AppsFlyer.AD}\": \"ad.mp4\"," else ""}
                ${if (addAdGroup) "\"${AttributionKeys.AppsFlyer.AD_GROUP}\": \"1111 - tm - aaa - US - 999 v1\"," else ""}
                "${AttributionKeys.AppsFlyer.ADSET}":  "0005 - tm - aaa - US - best 8",
                "${AttributionKeys.AppsFlyer.AD_KEYWORDS}": "keywords for ad",
                "${AttributionKeys.GPS_AD_ID}": $DEFAULT_GPSADID,
                "${AttributionKeys.IP}": $DEFAULT_IP,
                "${AttributionKeys.IDFA}": ${idfa?.let { "$it" }},
                "${AttributionKeys.IDFV}": ${idfv?.let { "$it" }}
            }
        """.trimIndent()
        )
    }

    private fun getBranchJSON(
        idfa: String? = DEFAULT_IDFA,
        idfv: String? = DEFAULT_IDFV,
    ): JSONObject {
        return JSONObject(
            """
            {
                "+is_first_session": false, 
                "+clicked_branch_link": false,
                "${AttributionKeys.Branch.CHANNEL}": "Facebook",
                "${AttributionKeys.Branch.CAMPAIGN}": "Facebook Ads 01293",
                "${AttributionKeys.GPS_AD_ID}": $DEFAULT_GPSADID,
                "${AttributionKeys.IP}": $DEFAULT_IP,
                "${AttributionKeys.IDFA}": ${idfa?.let { "$it" }},
                "${AttributionKeys.IDFV}": ${idfv?.let { "$it" }}
            }
        """.trimIndent()
        )
    }

    private fun getMParticleJSON(
        idfa: String? = DEFAULT_IDFA,
        idfv: String? = DEFAULT_IDFV,
        addMParticleId: Boolean = true,
        addNetworkID: Boolean = false,
    ): JSONObject {
        return JSONObject(
            """
            {
                ${if (addMParticleId) "\"${AttributionKeys.MParticle.ID}\": \"-2579252457900000000\"," else ""}
                ${if (addNetworkID) "\"${AttributionKeys.NETWORK_ID}\": \"10f0c0000aca0b00000fb0000c0f0f00\"," else ""}
                "${AttributionKeys.GPS_AD_ID}": $DEFAULT_GPSADID,
                "${AttributionKeys.IP}": $DEFAULT_IP,
                "${AttributionKeys.IDFA}": ${idfa?.let { "$it" }},
                "${AttributionKeys.IDFV}": ${idfv?.let { "$it" }}
            }
        """.trimIndent()
        )
    }

    private fun getFacebookOrTenjinJSON(
        idfa: String? = DEFAULT_IDFA,
        idfv: String? = DEFAULT_IDFV
    ): JSONObject {
        return JSONObject(
            """
            {
                "${AttributionKeys.GPS_AD_ID}": $DEFAULT_GPSADID,
                "${AttributionKeys.IP}": $DEFAULT_IP,
                "${AttributionKeys.IDFA}": ${idfa?.let { "$it" }},
                "${AttributionKeys.IDFV}": ${idfv?.let { "$it" }}
            }
        """.trimIndent()
        )
    }

    private fun checkCommonAttributes(
        converted: Map<String, String>,
        expectedIdfa: String? = DEFAULT_IDFA,
        expectedIdfv: String? = DEFAULT_IDFV,
        expectedIP: String = DEFAULT_IP,
        expectedGPSAdId: String = DEFAULT_GPSADID
    ) {
        assertThat(converted[SPECIAL_KEY_IDFA]).isEqualTo(expectedIdfa)
        assertThat(converted[SPECIAL_KEY_IDFV]).isEqualTo(expectedIdfv)
        assertThat(converted[SPECIAL_KEY_IP]).isEqualTo(expectedIP)
        assertThat(converted[SPECIAL_KEY_GPS_AD_ID]).isEqualTo(expectedGPSAdId)
    }
}