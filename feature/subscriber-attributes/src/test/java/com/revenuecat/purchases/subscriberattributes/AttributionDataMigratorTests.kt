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

    private lateinit var underTest: AttributionDataMigrator

    @Before
    fun setup() {
        underTest = AttributionDataMigrator()
    }

    @Test
    fun `Adjust attribution is properly converted`() {
        val jsonObject = JSONObject("""
            {
                "clickLabel": "",
                "trackerToken": "6abc940",
                "adid": "00f0c0000aca0b00000fb0000c0f0f00",
                "trackerName": "Instagram Profile::IG Spanish",
                "campaign": "IG Spanish",
                "adgroup": "",
                "creative": "",
                "network": "Instagram Profile",
                "rc_gps_adid": "e00000d0-0c0d-00b0-a000-acc0e00a0000",
                "rc_ip_address": "192.168.1.130",
                "rc_idfa": null
            }
        """.trimIndent())
        val converted =
            underTest.convertAttributionDataToSubscriberAttributes(data = jsonObject, AttributionNetwork.ADJUST)
        assertThat(converted).isNotNull
    }

}