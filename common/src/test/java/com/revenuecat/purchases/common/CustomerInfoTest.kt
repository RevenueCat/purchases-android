//  Purchases
//
//  Copyright © 2019 RevenueCat, Inc. All rights reserved.
//

package com.revenuecat.purchases.common

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.utils.Responses
import org.assertj.core.api.Assertions.assertThat
import org.json.JSONException
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class CustomerInfoTest {

    private val fullCustomerInfo: CustomerInfo by lazy {
        JSONObject(Responses.validFullPurchaserResponse).buildCustomerInfo()
    }

    @Test(expected = JSONException::class)
    fun failsToBeCreatedWithEmptyJSONObject() {
        val empty = JSONObject("{}")
        empty.buildCustomerInfo()
    }

    @Test
    @Throws(JSONException::class)
    fun `Given an empty response, empty object is created`() {
        val jsonObject = JSONObject(Responses.validEmptyPurchaserResponse)

        val info = jsonObject.buildCustomerInfo()

        assertThat(info).isNotNull
        assertThat(info.activeSubscriptions).isEmpty()
        assertThat(info.allPurchasedSkus).isEmpty()
        assertThat(info.nonSubscriptionTransactions).isEmpty()
        assertThat(info.latestExpirationDate).isNull()
    }

    @Suppress("DEPRECATION")
    @Test
    @Throws(JSONException::class)
    fun `Given a full response with non subscription SKUs, all SKUs are parsed properly`() {
        val info = fullCustomerInfo

        assertThat(info.purchasedNonSubscriptionSkus.size).isEqualTo(3)
        assertThat(info.purchasedNonSubscriptionSkus).contains("100_coins_pack")
        assertThat(info.purchasedNonSubscriptionSkus).contains("7_extra_lives")
        assertThat(info.purchasedNonSubscriptionSkus).contains("lifetime_access")

        assertThat(info.nonSubscriptionTransactions.size).isEqualTo(5)
        assertThat(info.nonSubscriptionTransactions.filter { it.productId == "100_coins_pack" }.size).isEqualTo(2)
        assertThat(info.nonSubscriptionTransactions.filter { it.productId == "7_extra_lives" }.size).isEqualTo(2)
        assertThat(info.nonSubscriptionTransactions.filter { it.productId == "lifetime_access" }.size).isEqualTo(1)
    }

    @Test
    @Throws(JSONException::class)
    fun `Given a full response, active subscription is calculated properly`() {
        val info = fullCustomerInfo
        val actives = info.activeSubscriptions

        assertThat(actives.size).isEqualTo(1)
        assertThat(actives).contains("onemonth_freetrial")
    }

    @Test
    @Throws(JSONException::class)
    fun `Given a full response, all purchased SKUs are retrieved properly`() {
        val info = fullCustomerInfo
        val purchasedSkus = info.allPurchasedSkus

        assertThat(purchasedSkus.size).isEqualTo(5)
        assertThat(purchasedSkus).contains("onemonth_freetrial")
        assertThat(purchasedSkus).contains("100_coins_pack")
        assertThat(purchasedSkus).contains("threemonth_freetrial")
        assertThat(purchasedSkus).contains("7_extra_lives")
        assertThat(purchasedSkus).contains("lifetime_access")
    }

    @Test
    @Throws(JSONException::class)
    fun `Given a full purchase info, latest expiration date is calculated properly`() {
        val info = fullCustomerInfo

        val latest = info.latestExpirationDate

        assertThat(latest).isNotNull()
        assertThat(latest!!.time).isEqualTo(4110728085975L)
    }

    @Test
    @Throws(JSONException::class)
    fun `Given a full purchase info, expiration date is de-serialized properly`() {
        val info = fullCustomerInfo

        val oneMonthDate = info.getExpirationDateForSku("onemonth_freetrial")
        val threeMonthDate = info.getExpirationDateForSku("threemonth_freetrial")

        assertThat(oneMonthDate!!.after(threeMonthDate)).`as`("$oneMonthDate is after $threeMonthDate")
            .isTrue()
    }

    @Test
    @Throws(JSONException::class)
    fun `Given two valid products, json is deserialized properly`() {
        val info = fullCustomerInfo
        assertThat(info).isNotNull
    }

    @Test
    @Throws(JSONException::class)
    fun `Given a full purchase info, expiration dates are retrieved properly`() {
        val info = fullCustomerInfo

        val pro = info.getExpirationDateForEntitlement("pro")
        val oldPro = info.getExpirationDateForEntitlement("old_pro")

        assertThat(pro!!.after(oldPro)).`as`("$pro is after $oldPro").isTrue()
    }

    @Test
    @Throws(JSONException::class)
    fun `Given a full purchase info, active entitlements are retrieved properly`() {
        val info = fullCustomerInfo
        val actives = info.entitlements.active.keys

        assertThat(actives.size).isEqualTo(2)

        assertThat(actives).contains("pro")
        assertThat(actives).doesNotContain("old_pro")
        assertThat(actives).contains("forever_pro")
        assertThat(actives).doesNotContain("random")
    }

    @Test
    @Throws(JSONException::class)
    fun `Given a null expiration date, expiration date is null`() {
        val info = fullCustomerInfo

        val foreverPro = info.getExpirationDateForEntitlement("forever_pro")

        assertThat(foreverPro).isNull()
    }

    @Test
    fun `Given a request date, it is de-serialized properly`() {
        val info = fullCustomerInfo
        assertThat(info.requestDate).isNotNull()
    }

    @Test
    fun `Given a valid purchaser info, purchase date is parsed`() {
        val info = fullCustomerInfo
        assertThat(info.getPurchaseDateForEntitlement("pro")).isNotNull()
    }

    @Test
    fun `Given two empty purchaser infos, both are equal`() {
        val info = JSONObject(Responses.validEmptyPurchaserResponse).buildCustomerInfo()
        val info1 = JSONObject(Responses.validEmptyPurchaserResponse).buildCustomerInfo()
        assertThat(info == info1).isTrue()
    }

    @Test
    fun `Given two empty purchaser infos with different request dates, both are equal`() {
        val jsonObject = JSONObject(Responses.validFullPurchaserResponse)
        jsonObject.put("request_date", "2018-06-20T06:24:50Z")
        val info = jsonObject.buildCustomerInfo()
        jsonObject.put("request_date", "2018-05-20T06:24:50Z")
        val info1 = jsonObject.buildCustomerInfo()
        assertThat(info).isEqualTo(info1)
    }

    @Test
    fun `Given two empty purchaser infos with different active entitlements, both are not equal`() {
        val jsonObject = JSONObject(Responses.validFullPurchaserResponse)
        val info = jsonObject.buildCustomerInfo()
        jsonObject.put("request_date", "2101-05-20T06:24:50Z")
        val info1 = jsonObject.buildCustomerInfo()

        assertThat(info).isNotEqualTo(info1)
    }

    @Test
    fun `Given two same purchaser infos, their hashcodes are the same`() {
        val jsonObject = JSONObject(Responses.validFullPurchaserResponse)
        val x = jsonObject.buildCustomerInfo()
        val y = jsonObject.buildCustomerInfo()
        assertThat(x.hashCode() == y.hashCode())
    }

    @Test
    fun `Management url is properly retrieved`() {
        val jsonObject = JSONObject(Responses.validFullPurchaserResponse)
        val x = jsonObject.buildCustomerInfo()
        assertThat(x.managementURL).isEqualTo(Uri.parse("https://play.google.com/store/account/subscriptions"))
    }

    @Test
    fun `If management url is null in the JSON, the purchaser info is properly built`() {
        val jsonObject = JSONObject(Responses.validFullPurchaserResponse)
        val subscriber = jsonObject.getJSONObject("subscriber")
        subscriber.put("management_url", JSONObject.NULL)
        jsonObject.put("subscriber", subscriber)
        val x = jsonObject.buildCustomerInfo()
        assertThat(x.managementURL).isNull()
    }

    @Test
    fun `If management url is missing in the JSON, it is null in the customerInfo`() {
        val jsonObject = JSONObject(Responses.validEmptyPurchaserResponse)
        val x = jsonObject.buildCustomerInfo()
        assertThat(x.managementURL).isNull()
    }

    @Test
    fun `Original purchase date is properly retrieved`() {
        val jsonObject = JSONObject(Responses.validFullPurchaserResponse)
        val x = jsonObject.buildCustomerInfo()
        assertThat(x.originalPurchaseDate!!.time).isEqualTo(1564183841000L)
    }

    @Test
    fun `Original purchase date is null if missing`() {
        val x = JSONObject(Responses.validEmptyPurchaserResponse).buildCustomerInfo()
        assertThat(x.originalPurchaseDate).isNull()
    }

    @Test
    fun `Original purchase date is null if it's present but it's a null string`() {
        val jsonObject = JSONObject(Responses.validFullPurchaserResponse)
        val subscriber = jsonObject.getJSONObject("subscriber")
        subscriber.put("original_purchase_date", JSONObject.NULL)
        jsonObject.put("subscriber", subscriber)

        val x = jsonObject.buildCustomerInfo()

        assertThat(x.originalPurchaseDate).isNull()
    }

    @Test
    fun `Non subscription transactions is empty if there are no non_subscriptions`() {
        val jsonObject = JSONObject(Responses.validEmptyPurchaserResponse)
        val x = jsonObject.buildCustomerInfo()

        assertThat(x.nonSubscriptionTransactions).isEmpty()
    }

    @Test
    fun `Non subscription transactions is correctly created`() {
        assertThat(fullCustomerInfo.nonSubscriptionTransactions).isNotEmpty
        assertThat(fullCustomerInfo.nonSubscriptionTransactions.size).isEqualTo(5)

        val oneTimePurchaseTransactions = fullCustomerInfo.nonSubscriptionTransactions.filter {
            it.productId == "100_coins_pack"
        }
        assertThat(oneTimePurchaseTransactions.size).isEqualTo(2)

        val consumableTransactions = fullCustomerInfo.nonSubscriptionTransactions.filter {
            it.productId == "7_extra_lives"
        }
        assertThat(consumableTransactions.size).isEqualTo(2)

        assertThat((oneTimePurchaseTransactions + consumableTransactions)
            .distinctBy { it.revenuecatId }.size).isEqualTo(4)
    }

    @Test
    fun `Non subscription transactions list is correctly created`() {
        assertThat(fullCustomerInfo.nonSubscriptionTransactions).isNotEmpty
        assertThat(fullCustomerInfo.nonSubscriptionTransactions.size).isEqualTo(5)
        assertThat((fullCustomerInfo.nonSubscriptionTransactions).distinctBy { it.revenuecatId }.size).isEqualTo(5)
    }
}
