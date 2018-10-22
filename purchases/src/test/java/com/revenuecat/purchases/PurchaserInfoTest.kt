package com.revenuecat.purchases

import android.support.test.runner.AndroidJUnit4
import org.json.JSONException
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

import org.assertj.core.api.Assertions.assertThat

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class PurchaserInfoTest {

    // FactoryTests
    private val factory = PurchaserInfo.Factory

    private fun fullPurchaserInfo(): PurchaserInfo {
        return factory.build(JSONObject(validFullPurchaserResponse))
    }

    @Test(expected = JSONException::class)
    fun failsToBeCreatedWithEmptyJSONObject() {
        val empty = JSONObject("{}")
        factory.build(empty)
    }

    @Test
    @Throws(JSONException::class)
    fun canCreateEmptyPurchaserInfo() {
        val jsonObject = JSONObject(validEmptyPurchaserResponse)

        val info = factory.build(jsonObject)

        assertThat(info).isNotNull
        assertThat(info.getActiveSubscriptions()).isEmpty()
        assertThat(info.getAllPurchasedSkus()).isEmpty()
        assertThat(info.purchasedNonSubscriptionSkus).isEmpty()
        assertThat(info.getLatestExpirationDate()).isNull()
    }

    @Test
    @Throws(JSONException::class)
    fun parsesOtherPurchases() {
        val info = fullPurchaserInfo()
        val nonSubscriptionSKUs = info.purchasedNonSubscriptionSkus

        assertThat(nonSubscriptionSKUs.size).isEqualTo(1)
        assertThat(nonSubscriptionSKUs).contains("onetime_purchase")
    }

    @Test
    @Throws(JSONException::class)
    fun getsActiveSubscriptions() {
        val info = fullPurchaserInfo()
        val actives = info.getActiveSubscriptions()

        assertThat(actives.size).isEqualTo(1)
        assertThat(actives).contains("onemonth_freetrial")
    }

    @Test
    @Throws(JSONException::class)
    fun getAllPurchasedSKUs() {
        val info = fullPurchaserInfo()
        val actives = info.getAllPurchasedSkus()

        assertThat(actives.size).isEqualTo(3)
        assertThat(actives).contains("onemonth_freetrial")
        assertThat(actives).contains("onetime_purchase")
        assertThat(actives).contains("threemonth_freetrial")
    }

    @Test
    @Throws(JSONException::class)
    fun getLatestExpirationDate() {
        val info = fullPurchaserInfo()

        val latest = info.getLatestExpirationDate()

        assertThat(latest).isNotNull()
        assertThat(latest!!.time).isEqualTo(4110728085975L)
    }

    @Test
    @Throws(JSONException::class)
    fun getExpirationDateForSku() {
        val info = fullPurchaserInfo()

        val oneMonthDate = info.getExpirationDateForSku("onemonth_freetrial")
        val threeMonthDate = info.getExpirationDateForSku("threemonth_freetrial")

        assertThat(oneMonthDate!!.after(threeMonthDate)).`as`("$oneMonthDate is after $threeMonthDate").isTrue()
    }

    @Test
    @Throws(JSONException::class)
    fun getExpirationDateTwoProducts() {
        val info = factory.build(JSONObject(validTwoProducts))
        assertThat(info).isNotNull
    }

    @Test
    @Throws(JSONException::class)
    fun getExpirationDateForEntitlement() {
        val info = fullPurchaserInfo()

        val pro = info.getExpirationDateForEntitlement("pro")
        val oldPro = info.getExpirationDateForEntitlement("old_pro")

        assertThat(pro!!.after(oldPro)).`as`("$pro is after $oldPro").isTrue()
    }

    @Test
    @Throws(JSONException::class)
    fun getsActiveEntitlements() {
        val info = fullPurchaserInfo()
        val actives = info.getActiveEntitlements()

        assertThat(actives.size).isEqualTo(2)

        assertThat(actives).contains("pro")
        assertThat(actives).doesNotContain("old_pro")
        assertThat(actives).contains("forever_pro")
        assertThat(actives).doesNotContain("random")
    }

    @Test
    @Throws(JSONException::class)
    fun getExpirationDateForForeverIsNull() {
        val info = fullPurchaserInfo()

        val foreverPro = info.getExpirationDateForEntitlement("forever_pro")

        assertThat(foreverPro).isNull()
    }

    companion object {

        internal const val validEmptyPurchaserResponse =
            "{'subscriber': {'other_purchases': {}, 'subscriptions': {}, 'entitlements': {}}}"
        internal const val validFullPurchaserResponse =
            "{'subscriber': {'other_purchases': {'onetime_purchase': {'purchase_date': '1990-08-30T02:40:36Z'}}, 'subscriptions': {'onemonth_freetrial': {'expires_date': '2100-04-06T20:54:45.975000Z'}, 'threemonth_freetrial': {'expires_date': '1990-08-30T02:40:36Z'}}, 'entitlements': { 'pro': {'expires_date': '2100-04-06T20:54:45.975000Z'}, 'old_pro': {'expires_date': '1990-08-30T02:40:36Z'}, 'forever_pro': {'expires_date': null}}}}"
        internal const val validTwoProducts =
            "{'subscriber': {'original_application_version': '1.0','other_purchases': {},'subscriptions':{'product_a': {'expires_date': '2018-05-27T06:24:50Z','period_type': 'normal'},'product_b': {'expires_date': '2018-05-27T05:24:50Z','period_type': 'normal'}}}}"
    }

}
