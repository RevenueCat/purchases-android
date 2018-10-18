package com.revenuecat.purchases

import android.support.test.runner.AndroidJUnit4
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

import org.assertj.core.api.Assertions.assertThat

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class EntitlementTest {

    private val factory = Entitlement

    @Test
    fun `given an empty case, factory can handle it`() {
        val entitlementMap = buildEntitlementMap("{}")
        assertThat(entitlementMap).`as`("check entitleMap is empty").isEmpty()
    }

    @Test
    fun `given an entitlement with no offerings, factory can handle it`() {
        val entitlementMap = buildEntitlementMap("{'pro': {'offerings': {}}}")
        assertThat(entitlementMap.size).`as`("check there is one entitle").isEqualTo(1)

        val e = entitlementMap["pro"]
        val offerings = e!!.offerings
        assertThat(offerings).`as`("check there is no offerings").isEmpty()
    }

    @Test
    fun `given an entitlement with one offering, factory can handle it`() {
        val entitlementMap =
            buildEntitlementMap("{'pro': {'offerings': {'monthly': {'active_product_identifier': 'onemonth_freetrial'}}}}")
        val e = entitlementMap["pro"]
        val offerings = e!!.offerings
        assertThat(offerings.size).`as`("check there is one offering").isEqualTo(1)
        val o = offerings["monthly"]
        assertThat(o!!.activeProductIdentifier)
            .`as`("check there is one offering onemonth_freetrial")
            .isEqualTo("onemonth_freetrial")
    }

    @Test
    fun `given an entitlement with multiple offerings, factory can handle it`() {
        val entitlementMap =
            buildEntitlementMap("{'pro': {'offerings': {'monthly': {'active_product_identifier': 'onemonth_freetrial'},'annual': {'active_product_identifier': 'oneyear_freetrial'}}}}")
        val e = entitlementMap["pro"]
        val offerings = e!!.offerings

        val monthly = offerings["monthly"]
        val annual = offerings["annual"]

        assertThat(monthly!!.activeProductIdentifier)
            .`as`("check there is one offering onemonth_freetrial")
            .isEqualTo("onemonth_freetrial")

        assertThat(annual!!.activeProductIdentifier)
            .`as`("check there is one offering oneyear_freetrial")
            .isEqualTo("oneyear_freetrial")
    }

    @Test
    fun `given multiple entitlements with multiple offerings, factory can handle it`() {
        val entitlementMap =
            buildEntitlementMap("{'pro': {'offerings': {'monthly': {'active_product_identifier': 'onemonth_freetrial'},'annual': {'active_product_identifier': 'oneyear_freetrial'}}}, 'pro2': {'offerings': {'monthly': {'active_product_identifier': 'onemonth_freetrial2'},'annual': {'active_product_identifier': 'oneyear_freetrial2'}}}}")
        assertThat(entitlementMap.size).`as`("check there is two entitles").isEqualTo(2)
        assertThat(entitlementMap["pro"]!!.offerings.size).`as`("check there is two pro offerings").isEqualTo(2)
        assertThat(entitlementMap["pro2"]!!.offerings.size).`as`("check there is two pro2 offerings").isEqualTo(2)
    }

    private fun buildEntitlementMap(json: String): Map<String, Entitlement> {
        return factory.build(JSONObject(json))
    }
}
