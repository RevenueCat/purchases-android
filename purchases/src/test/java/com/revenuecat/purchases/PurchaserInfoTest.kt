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
    fun `Given an empty response, empty object is created`() {
        val jsonObject = JSONObject(validEmptyPurchaserResponse)

        val info = factory.build(jsonObject)

        assertThat(info).isNotNull
        assertThat(info.activeSubscriptions).isEmpty()
        assertThat(info.allPurchasedSkus).isEmpty()
        assertThat(info.purchasedNonSubscriptionSkus).isEmpty()
        assertThat(info.latestExpirationDate).isNull()
    }

    @Test
    @Throws(JSONException::class)
    fun `Given a full response with non subscription SKUs, all SKUs are parsed properly`() {
        val info = fullPurchaserInfo()
        val nonSubscriptionSKUs = info.purchasedNonSubscriptionSkus

        assertThat(nonSubscriptionSKUs.size).isEqualTo(1)
        assertThat(nonSubscriptionSKUs).contains("onetime_purchase")
    }

    @Test
    @Throws(JSONException::class)
    fun `Given a full response, active subscription is calculated properly`() {
        val info = fullPurchaserInfo()
        val actives = info.activeSubscriptions

        assertThat(actives.size).isEqualTo(1)
        assertThat(actives).contains("onemonth_freetrial")
    }

    @Test
    @Throws(JSONException::class)
    fun `Given a full response, all purchased SKUs are retrieved properly`() {
        val info = fullPurchaserInfo()
        val actives = info.allPurchasedSkus

        assertThat(actives.size).isEqualTo(3)
        assertThat(actives).contains("onemonth_freetrial")
        assertThat(actives).contains("onetime_purchase")
        assertThat(actives).contains("threemonth_freetrial")
    }

    @Test
    @Throws(JSONException::class)
    fun `Given a full purchase info, latest expiration date is calculated properly`() {
        val info = fullPurchaserInfo()

        val latest = info.latestExpirationDate

        assertThat(latest).isNotNull()
        assertThat(latest!!.time).isEqualTo(4110728085975L)
    }

    @Test
    @Throws(JSONException::class)
    fun `Given a full purchase info, expiration date is de-serialized properly`() {
        val info = fullPurchaserInfo()

        val oneMonthDate = info.getExpirationDateForSku("onemonth_freetrial")
        val threeMonthDate = info.getExpirationDateForSku("threemonth_freetrial")

        assertThat(oneMonthDate!!.after(threeMonthDate)).`as`("$oneMonthDate is after $threeMonthDate").isTrue()
    }

    @Test
    @Throws(JSONException::class)
    fun `Given two valid products, json is deserialized properly`() {
        val info = factory.build(JSONObject(validTwoProducts))
        assertThat(info).isNotNull
    }

    @Test
    @Throws(JSONException::class)
    fun `Given a full purchase info, expiration dates are retrieved properly`() {
        val info = fullPurchaserInfo()

        val pro = info.getExpirationDateForEntitlement("pro")
        val oldPro = info.getExpirationDateForEntitlement("old_pro")

        assertThat(pro!!.after(oldPro)).`as`("$pro is after $oldPro").isTrue()
    }

    @Test
    @Throws(JSONException::class)
    fun `Given a full purchase info, active entitlements are retrieved properly`() {
        val info = fullPurchaserInfo()
        val actives = info.activeEntitlements

        assertThat(actives.size).isEqualTo(2)

        assertThat(actives).contains("pro")
        assertThat(actives).doesNotContain("old_pro")
        assertThat(actives).contains("forever_pro")
        assertThat(actives).doesNotContain("random")
    }

    @Test
    @Throws(JSONException::class)
    fun `Given a null expiration date, expiration date is null`() {
        val info = fullPurchaserInfo()

        val foreverPro = info.getExpirationDateForEntitlement("forever_pro")

        assertThat(foreverPro).isNull()
    }

    @Test
    fun `Given a request date, it is de-serialized properly`() {
        val info = fullPurchaserInfo()
        assertThat(info.requestDate).isNotNull()
    }

    @Test
    fun `Given a null request date, current date is used`() {
        val validNoRequestDate =
            "{'subscriber': {'other_purchases': {'onetime_purchase': {'purchase_date': '1990-08-30T02:40:36Z'}}, 'subscriptions': {'onemonth_freetrial': {'expires_date': '2100-04-06T20:54:45.975000Z'}, 'threemonth_freetrial': {'expires_date': '1990-08-30T02:40:36Z'}}, 'entitlements': { 'pro': {'expires_date': '2100-04-06T20:54:45.975000Z'}, 'old_pro': {'expires_date': '1990-08-30T02:40:36Z'}, 'forever_pro': {'expires_date': null}}}}"
        val info = factory.build(JSONObject(validNoRequestDate))

        val actives = info.allPurchasedSkus

        assertThat(actives.size).isEqualTo(3)
        assertThat(actives).contains("onemonth_freetrial")
        assertThat(actives).contains("onetime_purchase")
        assertThat(actives).contains("threemonth_freetrial")
    }

    @Test
    fun `Given a valid purchaser info, purchase date is parsed`() {
        val info = fullPurchaserInfo()
        assertThat(info.allPurchaseDatesByEntitlement).isNotEmpty
        assertThat(info.getPurchaseDateForEntitlement("pro")).isNotNull()
    }

    companion object {

        internal const val validEmptyPurchaserResponse =
            "{'request_date': '', 'subscriber': {'other_purchases': {}, 'subscriptions': {}, 'entitlements': {}}}"
        internal const val validFullPurchaserResponse =
            "{'request_date': '2018-10-19T02:40:36Z', 'subscriber': {'other_purchases': {'onetime_purchase': {'purchase_date': '1990-08-30T02:40:36Z'}}, 'subscriptions': {'onemonth_freetrial': {'expires_date': '2100-04-06T20:54:45.975000Z'}, 'threemonth_freetrial': {'expires_date': '1990-08-30T02:40:36Z'}}, 'entitlements': { 'pro': {'expires_date': '2100-04-06T20:54:45.975000Z', 'purchase_date': '2018-10-26T23:17:53Z'}, 'old_pro': {'expires_date': '1990-08-30T02:40:36Z'}, 'forever_pro': {'expires_date': null}}}}"
        internal const val validTwoProducts =
            "{'request_date': '2018-05-20T06:24:50Z', 'subscriber': {'original_application_version': '1.0','other_purchases': {},'subscriptions':{'product_a': {'expires_date': '2018-05-27T06:24:50Z','period_type': 'normal'},'product_b': {'expires_date': '2018-05-27T05:24:50Z','period_type': 'normal'}}}}"
    }

}
