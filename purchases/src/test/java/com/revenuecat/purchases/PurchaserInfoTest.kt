//  Purchases
//
//  Copyright © 2019 RevenueCat, Inc. All rights reserved.
//

package com.revenuecat.purchases

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.assertj.core.api.Assertions.assertThat
import org.json.JSONException
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class PurchaserInfoTest {

    private fun fullPurchaserInfo(): PurchaserInfo {
        return JSONObject(Responses.validFullPurchaserResponse).buildPurchaserInfo()
    }

    @Test(expected = JSONException::class)
    fun failsToBeCreatedWithEmptyJSONObject() {
        val empty = JSONObject("{}")
        empty.buildPurchaserInfo()
    }

    @Test
    @Throws(JSONException::class)
    fun `Given an empty response, empty object is created`() {
        val jsonObject = JSONObject(Responses.validEmptyPurchaserResponse)

        val info = jsonObject.buildPurchaserInfo()

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

        assertThat(oneMonthDate!!.after(threeMonthDate)).`as`("$oneMonthDate is after $threeMonthDate")
            .isTrue()
    }

    @Test
    @Throws(JSONException::class)
    fun `Given two valid products, json is deserialized properly`() {
        val info = JSONObject(Responses.validFullPurchaserResponse).buildPurchaserInfo()
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
    fun `Given a valid purchaser info, purchase date is parsed`() {
        val info = fullPurchaserInfo()
        assertThat(info.getPurchaseDateForEntitlement("pro")).isNotNull()
    }

    @Test
    fun `Given two empty purchaser infos, both are equal`() {
        val info = JSONObject(Responses.validEmptyPurchaserResponse).buildPurchaserInfo()
        val info1 = JSONObject(Responses.validEmptyPurchaserResponse).buildPurchaserInfo()
        assertThat(info == info1).isTrue()
    }

    @Test
    fun `Given two empty purchaser infos with different request dates, both are equal`() {
        val jsonObject = JSONObject(Responses.validFullPurchaserResponse)
        jsonObject.put("request_date", "2018-06-20T06:24:50Z")
        val info = jsonObject.buildPurchaserInfo()
        jsonObject.put("request_date", "2018-05-20T06:24:50Z")
        val info1 = jsonObject.buildPurchaserInfo()
        assertThat(info).isEqualTo(info1)
    }

    @Test
    fun `Given two empty purchaser infos with different active entitlements, both are not equal`() {
        val jsonObject = JSONObject(Responses.validFullPurchaserResponse)
        val info = jsonObject.buildPurchaserInfo()
        jsonObject.put("request_date", "2101-05-20T06:24:50Z")
        val info1 = jsonObject.buildPurchaserInfo()

        assertThat(info).isNotEqualTo(info1)
    }

    @Test
    fun `Given two same purchaser infos, their hashcodes are the same`() {
        val jsonObject = JSONObject(Responses.validFullPurchaserResponse)
        val x = jsonObject.buildPurchaserInfo()
        val y = jsonObject.buildPurchaserInfo()
        assertThat(x.hashCode() == y.hashCode())
    }

    @Test
    fun `Management url is properly retrieved`() {
        val jsonObject = JSONObject(Responses.validFullPurchaserResponse)
        val x = jsonObject.buildPurchaserInfo()
        assertThat(x.managementURL).isEqualTo(Uri.parse("https://play.google.com/store/account/subscriptions"))
    }

    @Test
    fun `If management url is null in the JSON, the purchaser info is properly built`() {
        val jsonObject = JSONObject(Responses.validFullPurchaserResponse)
        val subscriber = jsonObject.getJSONObject("subscriber")
        subscriber.put("management_url", JSONObject.NULL)
        jsonObject.put("subscriber", subscriber)
        val x = jsonObject.buildPurchaserInfo()
        assertThat(x.managementURL).isNull()
    }

    @Test
    fun `If management url is missing in the JSON, it is null in the purchaserInfo`() {
        val jsonObject = JSONObject(Responses.validEmptyPurchaserResponse)
        val x = jsonObject.buildPurchaserInfo()
        assertThat(x.managementURL).isNull()
    }

    @Test
    fun `Original purchase date is properly retrieved`() {
        val jsonObject = JSONObject(Responses.validFullPurchaserResponse)
        val x = jsonObject.buildPurchaserInfo()
        assertThat(x.originalPurchaseDate!!.time).isEqualTo(1564183841000L)
    }

    @Test
    fun `Original purchase date is null if missing`() {
        val x = JSONObject(Responses.validEmptyPurchaserResponse).buildPurchaserInfo()
        assertThat(x.originalPurchaseDate).isNull()
    }

    @Test
    fun `Original purchase date is null if it's present but it's a null string`() {
        val jsonObject = JSONObject(Responses.validFullPurchaserResponse)
        val subscriber = jsonObject.getJSONObject("subscriber")
        subscriber.put("original_purchase_date", JSONObject.NULL)
        jsonObject.put("subscriber", subscriber)

        val x = jsonObject.buildPurchaserInfo()

        assertThat(x.originalPurchaseDate).isNull()
    }

}
